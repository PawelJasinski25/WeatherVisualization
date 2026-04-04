import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import authService from "../services/authService.js";
import "../styles/auth.css";

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            await authService.login(email, password);
            navigate("/dashboard");
        } catch (error) {
            setError("Nieprawidłowe dane logowania");
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h2 className="auth-title">Logowanie</h2>

                <form onSubmit={handleLogin} className="auth-form">
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        required
                        className="auth-input"
                    />
                    <input
                        type="password"
                        placeholder="Hasło"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                        className="auth-input"
                    />
                    <button type="submit" className="auth-button">
                        Zaloguj się
                    </button>
                </form>

                {error && <p className="auth-error">{error}</p>}

                <div className="auth-link">
                    Nie masz konta? <Link to="/register">Zarejestruj się</Link>
                </div>
            </div>
        </div>
    );
};;

export default LoginPage;