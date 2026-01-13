import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from "../services/authService.js";

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

        if (password !== confirmPassword) {
            setError('Hasła muszą być identyczne.');
            return;
        }

        if (password.length < 4) {
            setError('Hasło jest za krótkie (min. 4 znaki).');
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
                setError('Użytkownik o takim adresie email już istnieje.');
            } else {
                setError('Wystąpił błąd podczas rejestracji. Spróbuj ponownie.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h2 style={styles.title}>Rejestracja</h2>

                <form onSubmit={handleRegister} style={styles.form}>
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        required
                        style={styles.input}
                    />
                    <input
                        type="password"
                        placeholder="Hasło"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                        style={styles.input}
                    />
                    <input
                        type="password"
                        placeholder="Powtórz hasło"
                        value={confirmPassword}
                        onChange={e => setConfirmPassword(e.target.value)}
                        required
                        style={styles.input}
                    />

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            ...styles.button,
                            background: loading ? "#ccc" : "#28a745",
                            cursor: loading ? "not-allowed" : "pointer"
                        }}
                    >
                        {loading ? 'Rejestrowanie...' : 'Utwórz konto'}
                    </button>
                </form>

                {error && <p style={styles.error}>{error}</p>}
                {successMsg && <p style={styles.success}>{successMsg}</p>}

                <p style={styles.footerText}>
                    Masz już konto? <Link to="/login" style={styles.link}>Zaloguj się</Link>
                </p>
            </div>
        </div>
    );
};

const styles = {
    container: {
        height: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        background: "#f0f2f5"
    },
    card: {
        background: "white",
        padding: "40px",
        borderRadius: "8px",
        boxShadow: "0 4px 12px rgba(0,0,0,0.1)",
        width: "350px",
        display: 'flex',
        flexDirection: 'column'
    },
    title: {
        textAlign: "center",
        marginBottom: "20px",
        color: "#333"
    },
    form: {
        display: "flex",
        flexDirection: "column",
        gap: "15px"
    },
    input: {
        padding: "12px",
        borderRadius: "6px",
        border: "1px solid #ddd",
        fontSize: "1rem"
    },
    button: {
        padding: "12px",
        color: "white",
        border: "none",
        borderRadius: "6px",
        fontWeight: "bold",
        fontSize: "1rem",
        transition: "background 0.2s"
    },
    error: {
        color: "#dc3545",
        marginTop: "15px",
        textAlign: "center",
        fontSize: "0.9em"
    },
    success: {
        color: "#28a745",
        marginTop: "15px",
        textAlign: "center",
        fontSize: "0.9em"
    },
    footerText: {
        marginTop: "20px",
        textAlign: "center",
        fontSize: "0.9em",
        color: "#666"
    },
    link: {
        color: "#007bff",
        textDecoration: "none",
        fontWeight: "500"
    }
};

export default RegisterPage;