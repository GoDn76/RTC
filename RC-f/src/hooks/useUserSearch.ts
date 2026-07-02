import { useQuery } from '@tanstack/react-query';
import { authApi } from '../api/auth';
import { useDebounce } from './useDebounce';

export function useUserSearch(query: string) {
  const debouncedQuery = useDebounce(query, 300);

  return useQuery({
    queryKey: ['users', debouncedQuery],
    queryFn: () => authApi.searchUsers(debouncedQuery),
    enabled: debouncedQuery.length >= 2,
    staleTime: 1000 * 60, // 1 minute
  });
}
