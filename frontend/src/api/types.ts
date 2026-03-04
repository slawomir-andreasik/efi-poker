// Auth

export interface AuthConfigResponse {
  auth0Enabled: boolean;
  registrationEnabled: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
}

export type UserRole = 'ADMIN' | 'USER';

export interface UserResponse {
  id: string;
  username: string;
  role: UserRole;
  hasPassword: boolean;
  authProvider: string;
}

// Admin

export interface AdminUserResponse {
  id: string;
  username: string;
  email: string | null;
  role: UserRole;
  authProvider: string;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface PagedUsersResponse {
  content: AdminUserResponse[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AdminCreateUserRequest {
  username: string;
  password: string;
  email?: string;
  role: UserRole;
}

export interface AdminUpdateUserRequest {
  email?: string;
  role?: UserRole;
}

export interface ChangePasswordRequest {
  currentPassword?: string;
  newPassword: string;
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

// Enums

export type StoryPoints = '0' | '0.5' | '1' | '2' | '3' | '5' | '8' | '13' | '21' | '?';

export const SP_VALUES: StoryPoints[] = ['0', '0.5', '1', '2', '3', '5', '8', '13', '21', '?'];

export type RoomStatus = 'OPEN' | 'REVEALED' | 'CLOSED';

export type RoomType = 'ASYNC' | 'LIVE';

// Responses

export interface ProjectResponse {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
}

export interface ProjectAdminResponse {
  id: string;
  name: string;
  slug: string;
  adminCode: string;
  createdAt: string;
}

export interface RoomResponse {
  id: string;
  slug: string;
  projectId: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  roomType: RoomType;
  status: RoomStatus;
  createdAt: string;
}

export interface RoomDetailResponse {
  id: string;
  slug: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  roomType: RoomType;
  status: RoomStatus;
  tasks: TaskWithEstimateResponse[];
}

export interface RoomAdminResponse {
  id: string;
  slug: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  roomType: RoomType;
  status: RoomStatus;
  tasks: TaskWithAllEstimatesResponse[];
  participants: ParticipantResponse[];
}

export interface RoomProgressResponse {
  roomId: string;
  slug: string;
  status: RoomStatus;
  totalParticipants: number;
  tasks: TaskProgressResponse[];
}

export interface RoomResultsResponse {
  roomId: string;
  slug: string;
  title: string;
  status: RoomStatus;
  tasks: TaskResultResponse[];
}

export interface RoomSlugResponse {
  roomId: string;
  roomTitle: string;
  roomSlug: string;
  projectSlug: string;
  projectName: string;
}

export interface TaskResponse {
  id: string;
  roomId: string;
  title: string;
  description?: string;
  sortOrder: number;
  finalEstimate: string | null;
  revealed: boolean;
  active: boolean;
  createdAt: string;
}

export interface TaskWithEstimateResponse {
  id: string;
  title: string;
  description?: string;
  sortOrder: number;
  myEstimate: EstimateResponse | null;
  allEstimates: EstimateResponse[];
  averagePoints: number | null;
  medianPoints: number | null;
  finalEstimate: string | null;
  votedCount: number;
  questionVotesCount?: number;
  totalParticipants: number;
  revealed: boolean;
  active: boolean;
}

export interface TaskWithAllEstimatesResponse {
  id: string;
  title: string;
  description?: string;
  sortOrder: number;
  finalEstimate: string | null;
  estimates: EstimateResponse[];
  revealed: boolean;
  active: boolean;
}

export interface TaskProgressResponse {
  taskId: string;
  title: string;
  votedCount: number;
  questionVotesCount?: number;
  totalParticipants: number;
}

export interface TaskResultResponse {
  taskId: string;
  title: string;
  estimates: EstimateResponse[];
  averagePoints: number;
  medianPoints: number;
  finalEstimate: string | null;
}

export interface EstimateResponse {
  id: string;
  participantId: string;
  participantNickname: string;
  storyPoints: string;
  comment?: string;
  createdAt: string;
}

export interface ParticipantResponse {
  id: string;
  nickname: string;
  invitedRoomIds?: string[];
  createdAt: string;
}

// Requests

export interface LiveRoomStateResponse {
  roomId: string;
  slug: string;
  title: string;
  topic: string | null;
  status: RoomStatus;
  roundNumber: number;
  taskId: string;
  myEstimate: EstimateResponse | null;
  questionVotesCount?: number;
  participants: LiveParticipantStatus[];
  results: LiveRoomResults | null;
}

export interface LiveParticipantStatus {
  participantId: string;
  nickname: string;
  hasVoted: boolean;
}

export interface LiveRoomResults {
  estimates: EstimateResponse[];
  averagePoints: number | null;
  medianPoints: number | null;
}

export interface NewRoundRequest {
  topic?: string;
}

export interface CreateProjectRequest {
  name: string;
}

export interface UpdateProjectRequest {
  name?: string;
}

export interface CreateRoomRequest {
  title: string;
  description?: string;
  deadline?: string;
  roomType: RoomType;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  sortOrder?: number;
}

export interface ImportTasksRequest {
  titles: string[];
}

export interface SubmitEstimateRequest {
  storyPoints: StoryPoints;
  comment?: string;
}

export interface JoinProjectRequest {
  nickname: string;
  roomId?: string;
}

export interface SetFinalEstimateRequest {
  storyPoints: StoryPoints;
}

// Round history

export interface RoundHistoryVote {
  nickname: string;
  storyPoints: string;
}

export interface RoundHistoryEntry {
  roundNumber: number;
  topic: string | null;
  averagePts: number | null;
  medianPts: number | null;
  voteCount: number;
  votes: RoundHistoryVote[];
  completedAt: string;
}
