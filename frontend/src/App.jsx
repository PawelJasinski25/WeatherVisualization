import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import DashboardPage from "./pages/DashboardPage.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import 'leaflet/dist/leaflet.css';
import TripsPage from "./pages/TripsPage.jsx";
import ReportPage from "./pages/ReportPage.jsx";
import AnimationPage from "./pages/AnimationPage.jsx";
import { UnitProvider } from "./contexts/UnitContext.jsx";

function App() {
    return (
        <UnitProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    <Route element={<ProtectedRoute />}>
                        <Route path="/dashboard" element={<DashboardPage />} />
                        <Route path="/trips" element={<TripsPage />} />
                        <Route path="/animation" element={<AnimationPage />} />
                        <Route path="/report" element={<ReportPage />} />
                    </Route>

                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </UnitProvider>
    );
}

export default App;

