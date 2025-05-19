import React, { useState } from "react";

function LoginForm({ onLogin, onRegister }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState(""); // only used for registration
  const [isRegistering, setIsRegistering] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isRegistering) {
      onRegister(username, password, email);
    } else {
      onLogin(username, password);
    }
  };

  return (
    <div className="login-form">
      <h2>{isRegistering ? "Register" : "Login"}</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          required
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        {isRegistering && (
          <input
            type="email"
            placeholder="Email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        )}
        <input
          type="password"
          placeholder="Password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit">{isRegistering ? "Register" : "Login"}</button>
      </form>
      <p>
        {isRegistering ? "Already have an account?" : "Don't have an account?"}
        <button
          type="button"
          onClick={() => setIsRegistering((prev) => !prev)}
        >
          {isRegistering ? "Login" : "Register"}
        </button>
      </p>
    </div>
  );
}

export default LoginForm;