import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import './App.css';
import { getApi } from './getApi';
import { useAuth } from "react-oidc-context";

function App() {
  const auth = useAuth();
  const [messages, setMessages] = useState([]);
  const [messageInput, setMessageInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [profilePictures, setProfilePictures] = useState({});
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);
  const api = getApi();

  const signOutRedirect = () => {
    const clientId = import.meta.env.VITE_CLIENT_ID;
    const logoutUri = import.meta.env.VITE_LOGOUT_URL;
    const cognitoDomain = import.meta.env.VITE_COGNITO_DOMAIN;
    window.location.href = `${cognitoDomain}/logout?client_id=${clientId}&logout_uri=${encodeURIComponent(logoutUri)}`;
  };


  if (auth.error) {
    return <div>Encountering error... {auth.error.message}</div>;
  }
  useEffect(() => {
    let interval;
  
    const startFetching = async () => {
      await fetchMessages(); 
  
      interval = setInterval(() => {
        fetchMessages(); 
      }, 3000); 
    };
  
    if (auth.isAuthenticated) {
      startFetching();
    }
  
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [auth.isAuthenticated]);
  useEffect(() => {
    if (auth.isAuthenticated && messages.length > 0) {
      loadProfilePictures();
    }
  }, [messages, auth.isAuthenticated]);

  const loadProfilePictures = async () => {
    const uniqueUsernames = [...new Set(messages.map(msg => msg.username))];
    const pictures = {};

    for (const username of uniqueUsernames) {
      if (!profilePictures[username]) {
        try {
          const pictureUrl = await api.getProfilePicture(username);
          if (pictureUrl) {
            pictures[username] = pictureUrl;
          }
        } catch (error) {
          console.error(`Error loading profile picture for ${username}:`, error);
        }
      }
    }

    setProfilePictures(prev => ({ ...prev, ...pictures }));
  };

  const fetchMessages = async () => {
    setLoading(true);
    try {
      const response = await api.fetchChats(auth.user?.profile?.['cognito:username']);
      if (response && typeof response === 'object' && Array.isArray(response.messages)) {
        setMessages(response.messages);
      } else {
        console.error('API response is not in the expected format:', response);
        setMessages([]);
      }
    } catch (error) {
      console.error('Error fetching messages:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSend = async () => {
    if (!messageInput.trim()) return;

    try {
      await api.sendMessage(messageInput, auth.user?.profile?.['cognito:username']);
      setMessageInput('');
      await fetchMessages();
    } catch (error) {
      console.error('Error sending message:', error);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSend();
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile || !auth.isAuthenticated) return;

    try {
      await api.uploadProfilePicture(
        auth.user?.profile?.['cognito:username'],
        selectedFile
      );
      // Refresh profile picture
      const newUrl = await api.getProfilePicture(auth.user?.profile?.['cognito:username']);
      setProfilePictures(prev => ({
        ...prev,
        [auth.user?.profile?.['cognito:username']]: newUrl
      }));
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (error) {
      console.error('Upload failed:', error);
    }
  };

  return (
    <div className="app">
      <div className="chat-container">
        {auth.isAuthenticated ? (
          <>
            <div className="header">
              <div className="user-info">
                {profilePictures[auth.user?.profile?.['cognito:username']] && (
                  <img
                    src={profilePictures[auth.user?.profile?.['cognito:username']]}
                    alt="Profile"
                    className="profile-pic"
                  />
                )}
                <span>Welcome, {auth.user?.profile?.['cognito:username']}</span>
              </div>
              <button className="sign-out-btn" onClick={() => auth.removeUser()}>
                Sign out
              </button>
            </div>

            <div className="messages-container">
              {messages.map((message, index) => (
                <div key={index} className="message">
                  {profilePictures[message.username] && (
                    <img
                      src={profilePictures[message.username]}
                      alt="Profile"
                      className="profile-pic"
                    />
                  )}
                  <div className="message-content">
                    <div className="message-sender">{message.username}</div>
                    <div className="message-text">{message.message}</div>
                  </div>
                </div>
              ))}
            </div>

            <div className="input-container">
              <input
                type="text"
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type a message..."
                disabled={loading}
              />
              <button onClick={handleSend} disabled={loading}>
                {loading ? 'Sending...' : 'Send'}
              </button>
            </div>

            <div className="profile-upload">
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept="image/*"
              />
              <button
                className="upload-btn"
                onClick={() => fileInputRef.current.click()}
              >
                Choose Profile Picture
              </button>
              {selectedFile && (
                <button className="upload-btn" onClick={handleUpload}>
                  Upload
                </button>
              )}
            </div>
          </>
        ) : (
          <div className="header">
            <span>Not logged in</span>
            <button onClick={() => auth.signinRedirect()}>Sign in</button>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;