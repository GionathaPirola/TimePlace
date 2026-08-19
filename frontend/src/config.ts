// Central place to read Vite env vars, so the rest of the app never touches import.meta.env directly.
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'
