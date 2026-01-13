import api from "../api/axios.js";

class AuthService{
    async login(email, password){
        const response = await api.post('/auth/login',{
            email, password
        });

        if (response.data.token){
            localStorage.setItem("token", response.data.token);
        }

        return response.data;
    }

    async register(email, password){
        return await api.post("/auth/register", {
            email, password
        });
    }

    logout(){
        localStorage.removeItem("token")
    }

    getCurrentUser(){
        return localStorage.getItem("token")
    }
}

export default new AuthService();
