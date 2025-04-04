import axios from 'axios';
import { env } from '$env/dynamic/public';

console.log('PUBLIC_API_BASE_URL:', env.PUBLIC_API_BASE_URL);

const api = axios.create({
	baseURL: env.PUBLIC_API_BASE_URL
});

export default api;
