export const environment = {
  production: true,
  apiUrl: (typeof window !== 'undefined' && (window as any).__API_URL__)
    ? (window as any).__API_URL__
    : '/api',
  useMockData: false,
  useBackend: true
};
