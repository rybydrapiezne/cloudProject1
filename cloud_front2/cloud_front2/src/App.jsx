import React, { useEffect, useRef, useState } from 'react';
import { getApi } from './getApi';
import LoginForm from './LoginForm';
import './App.css';

function App() {
  const [user, setUser] = useState(null); // Initialize user as null
  const [token, setToken] = useState(null); // Initialize token as null
  const [messages, setMessages] = useState([]);
  const [messageInput, setMessageInput] = useState('');
  const [profilePictures, setProfilePictures] = useState({});
  const [selectedFile, setSelectedFile] = useState(null);
  const [statuses, setStatuses] = useState({});
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [reactions, setReactions] = useState({});
  const fileInputRef = useRef(null);
  const api = getApi(token);

  const handleLogin = async (username, password) => {
    try {
      const loginApi = getApi(null); // no token yet
      const data = await loginApi.login(username, password);
      const token = data.accessToken;
      if (!token) throw new Error('No access token received');

      // Use this token to set status
      await getApi(token).setStatus(username, "online");

      // Now persist the state
      setUser(username);
      setToken(token);
      localStorage.setItem('username', username);
      localStorage.setItem('token', token);
    } catch (error) {
      console.error('Login failed', error);
      setUser(null);
      setToken(null);
      localStorage.removeItem('username');
      localStorage.removeItem('token');
      alert('Login failed: Invalid username or password');
    }
  };

  const handleRegister = async (username, password, email) => {
    try {
      const registerApi = getApi(null);
      const data = await registerApi.register(username, password, email);
      const token = data.accessToken;
      if (!token) throw new Error('No access token received');

      await getApi(token).setStatus(username, "online");

      setUser(username);
      setToken(token);
      localStorage.setItem('username', username);
      localStorage.setItem('token', token);
    } catch (error) {
      console.error('Registration failed', error);
      setUser(null);
      setToken(null);
      localStorage.removeItem('username');
      localStorage.removeItem('token');
      alert('Registration failed: Please try again');
    }
  };

  useEffect(() => {
    if (user) {
      fetchMessages();
      fetchOnlineUsers();
      const interval = setInterval(() => {
        fetchMessages();
        fetchOnlineUsers();
      }, 5000);
      return () => clearInterval(interval);
    }
  }, [user]);

  useEffect(() => {
    const loadPicturesAndStatuses = async () => {
      const usernames = [...new Set(messages.map(m => m.username))];
      const statusMap = {};
      const picMap = {};
      for (const uname of usernames) {
        if (!profilePictures[uname]) {
          picMap[uname] = await api.getProfilePicture(uname);
        }
        statusMap[uname] = await api.getStatus(uname);
      }
      setStatuses(statusMap);
      setProfilePictures(prev => ({ ...prev, ...picMap }));
    };
    if (messages.length > 0) loadPicturesAndStatuses();
  }, [messages]);

  const fetchMessages = async () => {
    try {
      const response = await api.fetchChats(user);
      console.log('Fetch messages response:', response);
      const messagesArray = Array.isArray(response.messages) ? response.messages : [];
      setMessages(messagesArray);
      const newReactions = {};
      for (const msg of messagesArray) {
        const r = await api.fetchReactions(msg.id);
        newReactions[msg.id] = r;
      }
      setReactions(newReactions);
    } catch (error) {
      console.error('Failed to fetch messages:', error);
      setMessages([]);
    }
  };

  const fetchOnlineUsers = async () => {
    try {
      const response = await api.getOnlineUsers();
      console.log('Raw API response:', response);
      
      const users = 
        Array.isArray(response) ? response :
        Array.isArray(response?.users) ? response.users :
        Array.isArray(response?.data?.users) ? response.data.users :
        [];
      
      console.log('Processed users:', users);
      setOnlineUsers(users);
    } catch (error) {
      console.error('Failed to fetch online users:', error);
      setOnlineUsers([]);
    }
  };

  const handleSend = async () => {
    if (!messageInput.trim()) return;
    await api.sendMessage(messageInput, user);
    setMessageInput('');
    await fetchMessages();
  };

  const handleReaction = async (msgId) => {
    const reaction = prompt('Enter emoji:');
    if (reaction) {
      await api.react(msgId, reaction, user);
      const updated = await api.fetchReactions(msgId);
      setReactions(prev => ({
        ...prev,
        [msgId]: updated
      }));
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    await api.uploadProfilePicture(user, selectedFile);
    const newUrl = await api.getProfilePicture(user);
    setProfilePictures(prev => ({ ...prev, [user]: newUrl }));
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleLogout = async () => {
    await api.setStatus(user, "offline");
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUser(null);
    setToken(null);
  };

  if (!user) return <LoginForm onLogin={handleLogin} onRegister={handleRegister} />;

  return (
    <div className="app">
      <div className="chat-container">
        <div className="header">
          <div className="user-info">
            {profilePictures[user] && <img src={profilePictures[user]} className="profile-pic" alt="Profile" />}
            <span>Welcome, {user}</span>
          </div>
          <button onClick={handleLogout}>Sign out</button>
        </div>

        <div className="main-content">
          <div className="messages-container">
            {messages.length > 0 ? (
              messages.map((msg, i) => (
                <div key={i} className="message">
                  {profilePictures[msg.username] && <img src={profilePictures[msg.username]} className="profile-pic" alt="Profile" />}
                  <div className="message-content">
                    <div className="message-sender">{msg.username} ({statuses[msg.username] || 'Offline'})</div>
                    <div className="message-text">{msg.message}</div>
                    <div className="message-reactions">
                      {reactions[msg.id] &&
                        Object.entries(reactions[msg.id]).map(([emoji, data], j) => (
                          <span key={j}>
                            {emoji} ({data.count})
                          </span>
                        ))}
                      <button onClick={() => handleReaction(msg.id)}>+</button>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <p>No messages available</p>
            )}
          </div>

          <div className="input-container">
            <input value={messageInput} onChange={e => setMessageInput(e.target.value)} placeholder="Type a message..." />
            <button onClick={handleSend}>Send</button>
          </div>

          <div className="profile-upload">
            <input type="file" ref={fileInputRef} onChange={handleFileChange} accept="image/*" />
            <button onClick={() => fileInputRef.current.click()}>Choose Picture</button>
            {selectedFile && <button onClick={handleUpload}>Upload</button>}
          </div>
        </div>

        <div className="online-users">
          <h3>Online Users</h3>
          {onlineUsers.length > 0 ? (
            onlineUsers.map((u, i) => <div key={i}>{u}</div>)
          ) : (
            <div>No online users</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;