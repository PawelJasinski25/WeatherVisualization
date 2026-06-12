import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import authService from "../services/authService.js";
import "../styles/auth.css";
import { CloudSun } from "lucide-react";

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');

        if (!email || !password) {
            setError("Wypełnij wszystkie pola.");
            return;
        }

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
                <div className="auth-brand-panel">
                    <CloudSun size={64} color="#ffffff" strokeWidth={1.5} />
                    <h1>Weather Visualization</h1>
                    <p>Analizuj pogodę na mapie, odtwarzaj animacje i generuj raporty PDF</p>
                </div>
                <div className="auth-form-panel">
                    <h2 className="auth-title">Logowanie</h2>

                    <form onSubmit={handleLogin} className="auth-form" noValidate>
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
        </div>

    );
};

export default LoginPage;