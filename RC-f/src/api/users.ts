import { fetchWithAuth } from './base';
import type { UserProfileDto } from '../types';

export const userApi = {
  searchUsers: (query: string): Promise<UserProfileDto[]> =>
    fetchWithAuth(`/api/profile/users?query=${encodeURIComponent(query)}`, {
      method: 'GET',
    }),
};
