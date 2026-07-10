const getApiBaseUrl = () => {
  let url = import.meta.env.VITE_API_BASE_URL || '';
  if (typeof window !== 'undefined' && url.includes('localhost')) {
    url = url.replace('localhost', window.location.hostname);
  }
  return url;
};

export const env = {
  apiBaseUrl: getApiBaseUrl(),
  googleClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || '',
  googleRedirectUri: import.meta.env.VITE_GOOGLE_REDIRECT_URI || 'http://localhost:5173/oauth/callback',
};
