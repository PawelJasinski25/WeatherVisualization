import { Navigate, Outlet } from 'react-router-dom';
import authService from "../services/authService.js";

const ProtectedRoute = () => {
    const user = authService.getCurrentUser();
    return user ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;