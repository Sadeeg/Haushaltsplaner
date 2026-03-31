export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  hasTelegram: boolean;
  householdId: number | null;
  householdName: string | null;
}

export interface Task {
  id: number;
  name: string;
  frequency: TaskFrequency;
  dueDate: string;
  completionPeriodStart: string | null;
  completionPeriodEnd: string | null;
  status: TaskStatus;
  assignedUserId: number | null;
  assignedUserName: string | null;
  points: number;
  completedAt: string | null;
}

export interface TaskTemplate {
  id: number;
  name: string;
  frequency: TaskFrequency;
  defaultPoints: number;
  completionPeriodDays: number | null;
}

export interface ExclusionRule {
  id: number;
  taskATemplateId: number;
  taskAName: string;
  taskBTemplateId: number;
  taskBName: string;
  ruleType: ExclusionType;
}

export interface LeaderboardEntry {
  userId: number;
  displayName: string;
  totalPoints: number;
  completedTasks: number;
  skippedTasks: number;
}

export interface CreateTaskRequest {
  name: string;
  frequency: TaskFrequency;
  dueDate?: string;
  completionPeriodStart?: string;
  completionPeriodEnd?: string;
  assignedUserId?: number;
  points?: number;
}

export enum TaskFrequency {
  DAILY = 'DAILY',
  WEEKLY = 'WEEKLY',
  BI_WEEKLY = 'BI_WEEKLY',
  MONTHLY = 'MONTHLY'
}

export enum TaskStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  SKIPPED = 'SKIPPED',
  MOVED = 'MOVED'
}

export enum ExclusionType {
  MUTUAL = 'MUTUAL',
  ONE_WAY = 'ONE_WAY'
}
