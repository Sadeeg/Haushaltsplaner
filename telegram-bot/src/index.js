const TelegramBot = require('node-telegram-bot-api');
const axios = require('axios');

const botToken = process.env.TELEGRAM_BOT_TOKEN || 'YOUR_BOT_TOKEN';
const apiUrl = process.env.API_URL || 'http://spring:8080/api';

const bot = new TelegramBot(botToken, { polling: true });

console.log('Telegram Bot started!');

// Store user chat IDs for daily notifications
const userChatIds = new Map();

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
      // Store the chat ID for daily notifications
      userChatIds.set(response.data.id, chatId);
      
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

// Daily morning notification at 08:00
async function sendMorningNotifications() {
  console.log('Sending morning notifications...');
  
  try {
    // Get all users with Telegram chat IDs
    const usersResponse = await axios.get(`${apiUrl}/users`);
    const users = usersResponse.data;
    
    for (const user of users) {
      if (!user.telegramChatId) continue;
      
      const chatId = user.telegramChatId;
      
      // Get today's tasks for this user
      const todayTasks = await axios.get(`${apiUrl}/tasks/user/${user.id}/date/${new Date().toISOString().split('T')[0]}`)
        .catch(() => ({ data: [] }));
      
      // Get this week's tasks (upcoming, not today)
      const weekTasks = await axios.get(`${apiUrl}/tasks/household/${user.householdId}/week`)
        .catch(() => ({ data: [] }));
      
      const upcomingTasks = weekTasks.data.filter(t => 
        t.assignedUserId === user.id && 
        t.status === 'PENDING' &&
        t.dueDate !== new Date().toISOString().split('T')[0]
      );
      
      // Get leaderboard for points comparison
      const leaderboard = await axios.get(`${apiUrl}/tasks/leaderboard?householdId=${user.householdId}`)
        .catch(() => ({ data: [] }));
      
      // Build the message
      let message = `🌅 *Guten Morgen ${user.displayName}!*\n\n`;
      
      if (todayTasks.data.length > 0) {
        message += `*Heute für dich:*\n`;
        todayTasks.data.forEach(t => {
          message += `☐ ${t.name}\n`;
        });
        message += '\n';
      }
      
      if (upcomingTasks.length > 0) {
        message += `*Diese Woche noch:*\n`;
        upcomingTasks.forEach(t => {
          const dueDate = t.completionPeriodEnd ? new Date(t.completionPeriodEnd).toLocaleDateString('de-DE', { weekday: 'short', day: 'numeric', month: 'short' }) : '';
          message += `☐ ${t.name}${dueDate ? ` (bis ${dueDate})` : ''}\n`;
        });
        message += '\n';
      }
      
      if (leaderboard.data.length > 0) {
        message += `*Dein Punktestand:*\n`;
        leaderboard.data.forEach((entry, index) => {
          const star = entry.userId === user.id ? '⭐' : '  ';
          message += `${star} ${entry.displayName}: ${entry.totalPoints} Punkte\n`;
        });
      }
      
      if (todayTasks.data.length === 0 && upcomingTasks.length === 0) {
        message += '🎉 Du hast diese Woche keine offenen Aufgaben!';
      }
      
      bot.sendMessage(chatId, message, { parse_mode: 'Markdown' });
    }
  } catch (error) {
    console.error('Error sending morning notifications:', error.message);
  }
}

// Schedule daily notification at 08:00
function scheduleMorningNotification() {
  const now = new Date();
  const scheduledTime = new Date(now);
  scheduledTime.setHours(8, 0, 0, 0); // 08:00
  
  // If it's already past 08:00, schedule for tomorrow
  if (now > scheduledTime) {
    scheduledTime.setDate(scheduledTime.getDate() + 1);
  }
  
  const msUntilNotification = scheduledTime.getTime() - now.getTime();
  
  console.log(`Next morning notification scheduled in ${Math.round(msUntilNotification / 1000 / 60)} minutes`);
  
  setTimeout(() => {
    sendMorningNotifications();
    // Then reschedule every 24 hours
    setInterval(sendMorningNotifications, 24 * 60 * 60 * 1000);
  }, msUntilNotification);
}

// Start the scheduler
scheduleMorningNotification();

// Error handling
bot.on('polling_error', (error) => {
  console.error('Polling error:', error.message);
});

module.exports = bot;
