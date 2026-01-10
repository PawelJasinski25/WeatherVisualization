import {useState} from "react";

const FileUpload = ({onUploadSuccess}) =>{
    const [file, setFile] = useState(null);
    const [status, setStatus] = useState("")

    const handleFileChange = (e) =>{
        setFile(e.target.files[0]);
        setStatus("");
    }

    const handleUpload = async () => {
        if (!file) {
            alert("Proszę wybrać plik")
            return;
        }

        setStatus("Wgrywanie...")

        const formData = new FormData();
        formData.append("file", file);

        try {
            // Strzał do Twojego Backendu
            const response = await fetch("http://localhost:8080/api/trips/upload", {
                method: "POST",
                body: formData,
            });

            if(response.ok){
            const newTripId = await response.json()
            setStatus("Sukces! ID: " + newTripId);
            onUploadSuccess(newTripId);
            }
            else{
                setStatus("Bład wgrywania pliku");
            }
        }
        catch (error){
            console.error("Błąd sieci:", error);
            setStatus("Nie można połączyć z serwerem.");
            }

        }


return (
    <div style={{ padding: "20px", width: "300px", background: "#f4f4f4", borderRight: "2px solid #ddd" }}>
        <h2>1. Wgraj GPX</h2>
        <input type="file" accept=".gpx" onChange={handleFileChange} />
        <br /><br />
        <button
            onClick={handleUpload}
            style={{ padding: "10px 20px", background: "#007bff", color: "white", border: "none", cursor: "pointer" }}
        >
            Wyślij na serwer
        </button>
        <p style={{ marginTop: "10px", fontWeight: "bold" }}>{status}</p>
    </div>
);
};

export default FileUpload;