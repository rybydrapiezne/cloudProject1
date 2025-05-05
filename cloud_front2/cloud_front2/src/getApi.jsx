import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;


export const getApi = () => {
    return {
        fetchChats: async (username) => {
            try {
                const response = await axios.get(`${API_BASE_URL}/chat/all`, {
                    params: {
                        username: username,
                    },
                    headers: {
                        'Content-Type': 'application/json',
                    },
                });
                console.log(response)
                return response.data;
            } catch (error) {
                console.error('Error fetching chats:', error);
                throw error;
            }
        },
        sendMessage: async (message, user) => {
            try {
                const newMessage = {
                    message: message,
                    username: user
                };
                const response = await axios.post(`${API_BASE_URL}/chat`, newMessage, {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                });
                return response.data;
            } catch (error) {
                console.error('Error sending message:', error);
                throw error;
            }
        },
        getProfilePicture: async (username) => {
            try {
                const response = await axios.get(`${API_BASE_URL}/profile/${username}/image`, {
                    responseType: 'blob' // Important for image data
                });
                return URL.createObjectURL(response.data);
            } catch (error) {
                console.error('Error fetching profile picture:', error);
                return null;
            }
        },

        uploadProfilePicture: async (username, file) => {
            const formData = new FormData();
            formData.append('username', username);
            formData.append('file', file);

            try {
                const response = await axios.post(`${API_BASE_URL}/profile/upload`, formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                });
                return response.data;
            } catch (error) {
                console.error('Error uploading profile picture:', error);
                throw error;
            }
        }
    };
};