const TelegramBot = require('node-telegram-bot-api');
const axios = require('axios');

const botToken = process.env.TELEGRAM_BOT_TOKEN || 'YOUR_BOT_TOKEN';
const apiUrl = process.env.API_URL || 'http://spring:8080/api';

const bot = new TelegramBot(botToken, { polling: true });

console.log('Telegram Bot started!');

// Handle /start command
bot.onText(/\/start/, async (msg) => {
  const chatId = msg.chat.id;
  const userId = msg.from.id;

  bot.sendMessage(chatId, 
    '🏠 *Haushaltsplaner Bot*\n\n' +
    'Willkommen! Mit diesem Bot erhältst du täglich deine Aufgaben.\n\n' +
    'Um deinen Account zu verknüpfen, gib den Code aus der App ein.',
    { parse_mode: 'Markdown' }
  );
});

// Handle verification code
bot.onText(/^[A-Z]{3}-[0-9]{3}-[A-Z0-9]{3}$/, async (msg) => {
  const chatId = msg.chat.id;
  const code = msg.text;

  try {
    // Link Telegram account via API
    const response = await axios.post(`${apiUrl}/users/telegram/link`, {}, {
      params: { code, telegramChatId: chatId }
    });

    if (response.data) {
      bot.sendMessage(chatId, 
        `✅ *Account verknüpft!*\n\n` +
        `Hallo ${response.data.displayName}!\n` +
        `Du erhältst jetzt deine täglichen Erinnerungen.`,
        { parse_mode: 'Markdown' }
      );
    }
  } catch (error) {
    bot.sendMessage(chatId, 
      '❌ *Fehler*\n\n' +
      'Der Code ist ungültig oder abgelaufen. Bitte fordere einen neuen Code in der App an.',
      { parse_mode: 'Markdown' }
    );
  }
});

// Handle /tasks command
bot.onText(/\/tasks/, async (msg) => {
  const chatId = msg.chat.id;

  try {
    // Get user by telegram chat ID
    const userResponse = await axios.get(`${apiUrl}/users/telegram/${chatId}`);
    const user = userResponse.data;

    if (!user) {
      bot.sendMessage(chatId, 'Bitte verknüpfe zuerst deinen Account mit dem Code.');
      return;
    }

    // Get today's tasks
    const tasksResponse = await axios.get(`${apiUrl}/tasks/user/${user.id}/date/${new Date().toISOString().split('T')[0]}`);
    const tasks = tasksResponse.data;

    if (tasks.length === 0) {
      bot.sendMessage(chatId, '🎉 Du hast heute keine Aufgaben!');
      return;
    }

    const taskList = tasks.map(t => `• ${t.name} (${t.points} Pkt.)`).join('\n');
    bot.sendMessage(chatId, 
      `📋 *Heutige Aufgaben:*\n\n${taskList}`,
      { parse_mode: 'Markdown' }
    );
  } catch (error) {
    console.error('Error fetching tasks:', error.message);
    bot.sendMessage(chatId, 'Fehler beim Laden der Aufgaben.');
  }
});

// Handle /help command
bot.onText(/\/help/, (msg) => {
  const chatId = msg.chat.id;
  bot.sendMessage(chatId,
    '📚 *Hilfe*\n\n' +
    '/start - Bot starten\n' +
    '/tasks - Heutige Aufgaben anzeigen\n' +
    '/help - Diese Hilfe anzeigen\n\n' +
    'Erhalte jeden Morgen um 8 Uhr deine Aufgaben!',
    { parse_mode: 'Markdown' }
  );
});

// Error handling
bot.on('polling_error', (error) => {
  console.error('Polling error:', error.message);
});

module.exports = bot;
