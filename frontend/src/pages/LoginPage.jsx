import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import authService from "../services/authService.js";

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
        <div style={styles.container}>
            <div style={styles.card}>
                <h2 style={styles.title}>Logowanie</h2>

                <form onSubmit={handleLogin} style={styles.form}>
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
                    <button type="submit" style={styles.button}>
                        Zaloguj
                    </button>
                </form>

                {error && <p style={styles.error}>{error}</p>}

                <p style={styles.footerText}>
                    Nie masz konta? <Link to="/register" style={styles.link}>Zarejestruj się</Link>
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
        background: "#007bff",
        color: "white",
        border: "none",
        borderRadius: "6px",
        cursor: "pointer",
        fontWeight: "bold",
        fontSize: "1rem",
        transition: "background 0.2s"
    },
    error: {
        color: "red",
        marginTop: "15px",
        textAlign: "center",
        fontSize: "0.9rem"
    },
    footerText: {
        marginTop: "20px",
        textAlign: "center",
        fontSize: "0.9rem",
        color: "#666"
    },
    link: {
        color: "#007bff",
        textDecoration: "none",
        fontWeight: "500"
    }
};

export default LoginPage;