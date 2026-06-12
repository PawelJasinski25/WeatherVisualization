import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from "../services/authService.js";
import "../styles/auth.css";
import { CloudSun } from "lucide-react";

const RegisterPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMsg('');

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            setError('Podaj poprawny format adresu e-mail.');
            return;
        }

        if (password !== confirmPassword) {
            setError('Hasła muszą być identyczne.');
            return;
        }

        if (password.length < 8) {
            setError('Hasło jest za krótkie (min. 8 znaków).');
            return;
        }

        setLoading(true);

        try {
            await authService.register(email, password);
            setSuccessMsg('Rejestracja udana! Za chwilę zostaniesz przekierowany do logowania...');
            setTimeout(() => {
                navigate('/login');
            }, 2000);
        } catch (err) {
            console.error(err);
            if (err.response && err.response.status === 409) {
                setError('Użytkownik z takim adresem email już istnieje.');
            } else {
                setError('Wystąpił błąd podczas rejestracji. Spróbuj ponownie.');
            }
        } finally {
            setLoading(false);
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
                    <h2 className="auth-title">Rejestracja</h2>

                    <form onSubmit={handleRegister} className="auth-form" noValidate>
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Hasło"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Powtórz hasło"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <button type="submit" disabled={loading} className="auth-button">
                            {loading ? 'Rejestrowanie...' : 'Zarejestruj się'}
                        </button>
                    </form>

                    {error && <p className="auth-error">{error}</p>}
                    {successMsg && <p className="auth-success">{successMsg}</p>}

                    <div className="auth-link">
                        Masz już konto? <Link to="/login">Zaloguj się</Link>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;