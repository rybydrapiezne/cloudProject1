import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// Create an axios instance that adds the token to headers
const createApiInstance = (token) => {
    return axios.create({
        baseURL: API_BASE_URL,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
    });
};

export const getApi = (token) => {
    const api = createApiInstance(token);

    return {
        fetchChats: async (username) => {
            const response = await api.get(`/chat/all`, { params: { username } });
            return response.data;
        },
        sendMessage: async (message, username) => {
            const response = await api.post(`/chat`, { message, username });
        try {
                await api.post(`/notifications/email`, {
                    target: "szymik43@gmail.com",
                    message: `New message from ${username}: "${message}"`
                });
            } catch (e) {
        console.error("Failed to send notification email:", e);
    }
            return response.data;
        },
        getProfilePicture: async (username) => {
            const res = await api.get(`/profile/${username}/image`, { responseType: 'blob' });
            return URL.createObjectURL(res.data);
        },
        uploadProfilePicture: async (username, file) => {
            const formData = new FormData();
            formData.append('username', username);
            formData.append('file', file);
            const response = await api.post(`/profile/upload`, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                    'Authorization': `Bearer ${token}`
                }
            });
            return response.data;
        },
        login: async (username, password) => {
            const response = await axios.post(`${API_BASE_URL}/auth/login`, {
                username,
                password
            });
            return response.data;
        },
        register: async (username, password, email) => {
            const response = await axios.post(`${API_BASE_URL}/auth/register`, {
                username,
                password,
                email
            });
            return response.data;
        },
        // The rest of the methods follow same pattern:
        react: async (msgId, emoji, username) => {
            const response = await api.post(`/chat/${msgId}/reactions`, { reaction: emoji, username });
            return response.data;
        },
        deleteReaction: async (msgId, emoji, username) => {
            const response = await api.delete(`/chat/${msgId}/reactions`, {
                data: { reaction: emoji, username }
            });
            return response.data;
        },
        fetchReactions: async (msgId) => {
            const response = await api.get(`/chat/${msgId}/reactions`);
            return response.data;
        },
        sendNotificationEmail: async (email, msg) => {
            const response = await api.post(`/notifications/email`, { target: email, message: msg });
            return response.data;
        },
        sendNotificationSms: async (phone, msg) => {
            const response = await api.post(`/notifications/sms`, { target: phone, message: msg });
            return response.data;
        },
        setStatus: async (username, status) => {
            const response = await api.post(`/profile/${username}/status`, { status });
            return response.data;
        },
        getStatus: async (username) => {
            const response = await api.get(`/profile/${username}/status`);
            return response.data;
        },
        getOnlineUsers: async () => {
            const response = await api.get(`/profile/online-users`);
            return response.data;
        }
    };
};
