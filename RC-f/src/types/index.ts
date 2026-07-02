export interface UserProfileDto {
  uuid: string;
  name: string;
  email: string;
  emailVerified?: boolean;
  avatar?: string;
}

export interface AuthResponseDto {
  accessToken: string;
  tokenType: string;
}

export interface ApiResponseDto {
  success: boolean;
  message: string;
}

export interface OtpVerificationDto {
  email: string;
  otp: string;
}

export interface ResetPasswordDto {
  email: string;
  otp: string;
  newPassword: string;
}

export interface EmailDto {
  email: string;
}

export interface GoogleLoginDto {
  idToken: string;
}

export interface User {
  id: string; // mapped from uuid
  name: string;
  email: string;
  avatar?: string;
  emailVerified?: boolean;
}

export interface Conversation {
  id: string; // Maps to roomId
  roomId: string;
  roomType: "GROUP" | "PRIVATE";
  displayName: string;
  avatar?: string; 

  // Extra optional metadata
  creator?: string;
  creatorId?: string;
  createdAt?: string;
  targetUserId?: string;

  // Chat State
  unreadCount: number;
  lastMessage?: string;
  lastMessageTime?: number;
}

export interface ChatMessage {
  id: string;
  roomId: string;
  userId: string;
  name: string;
  message: string;
  upvotes: number;
  timestamp: number;
}

export type WsAction = "CREATE_ROOM" | "CHAT" | "GET_CHATS" | "GET_ROOMS" | "JOIN";

export interface WsPayloadCreateRoom {
  action: "CREATE_ROOM";
  roomName?: string; // For Group Room
  targetUserId?: string; // Only for DM
}

export interface WsPayloadChat {
  action: "CHAT";
  message: string;
  roomId?: string; // Only for GROUP
  targetUserId?: string; // Only for DM
}

export interface WsPayloadGetChats {
  action: "GET_CHATS";
  limit: number;
  offset: number;
  roomId?: string; // Only for GROUP
  targetUserId?: string; // Only for DM
}

export interface WsPayloadGetRooms {
  action: "GET_ROOMS";
}

export interface WsPayloadJoinRoom {
  action: "JOIN";
  roomId: string;
}

export type WsOutgoingMessage = WsPayloadCreateRoom | WsPayloadChat | WsPayloadGetChats | WsPayloadGetRooms | WsPayloadJoinRoom;

export type WsEventType = "ROOM_CREATED" | "ADD_CHAT" | "UPDATE_CHAT" | "HISTORY" | "ERROR" | "ROOMS" | "JOIN_SUCCESS";

export interface WsEventRoomCreated {
  type: "ROOM_CREATED";
  payload: {
    roomId?: string; // Sometimes optional for DM? Wait, the user said it returns roomId for both.
    roomName?: string;
    creator?: string;
    creatorId?: string;
    createdAt?: string;
    roomType?: string;
    userId?: string; // For DM target user id ?
    targetUserId?: string; // Let's support both just in case
  };
}

export interface WsEventAddChat {
  type: "ADD_CHAT";
  payload: ChatMessage;
}

export interface WsEventUpdateChat {
  type: "UPDATE_CHAT";
  payload: ChatMessage;
}

export interface WsEventHistory {
  type: "HISTORY";
  payload: ChatMessage[];
}

export interface WsEventError {
  type: "ERROR";
  payload: string;
}

export interface WsEventRooms {
  type: "ROOMS";
  payload: Array<{
    roomId: string;
    roomType: "GROUP" | "PRIVATE";
    displayName: string;
    targetUserId?: string;
  }>;
}

export interface WsEventJoinSuccess {
  type: "JOIN_SUCCESS";
  payload: string;
}

export type WsIncomingMessage = 
  | WsEventRoomCreated 
  | WsEventAddChat 
  | WsEventUpdateChat 
  | WsEventHistory 
  | WsEventError
  | WsEventRooms
  | WsEventJoinSuccess;
