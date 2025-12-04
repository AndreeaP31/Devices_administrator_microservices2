// frontend/src/pages/ClientDashboard.jsx
import { useEffect, useState } from "react";
import { getDevicesForUser, getConsumption } from "../api";
import ChartComponent from "../components/ChartComponent"; // Asigură-te că creezi fișierul

export default function ClientDashboard({ user }) {
    const [devices, setDevices] = useState([]);
    const [selectedDevice, setSelectedDevice] = useState(null);
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]); // Default Azi (format YYYY-MM-DD)
    const [chartData, setChartData] = useState([]);

    // 1. Încarcă dispozitivele
    useEffect(() => {
        if(user && user.userId) {
            getDevicesForUser(user.userId).then(setDevices);
        }
    }, [user]);

    // 2. Când se selectează un device sau o dată, încarcă datele de consum
    useEffect(() => {
        if (selectedDevice && selectedDate) {
            const timestamp = new Date(selectedDate).getTime();
            getConsumption(selectedDevice, timestamp).then(data => {
                if(data) setChartData(data);
                else setChartData([]);
            });
        }
    }, [selectedDevice, selectedDate]);

    return (
        <div className="container">
            <div className="card">
                <h2>Your Devices</h2>

                {/* LISTA DEVICE-URI - Click pe unul pentru a selecta */}
                <div style={{ marginBottom: 20 }}>
                    {devices.map(d => (
                        <button
                            key={d.id}
                            onClick={() => setSelectedDevice(d.id)}
                            style={{
                                margin: "5px",
                                background: selectedDevice === d.id ? "#273c75" : "#eee",
                                color: selectedDevice === d.id ? "white" : "black"
                            }}
                        >
                            {d.name}
                        </button>
                    ))}
                </div>

                {/* CALENDAR SI GRAFIC (Doar dacă e selectat un device) */}
                {selectedDevice && (
                    <div>
                        <h3>Select Date</h3>
                        <input
                            type="date"
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            style={{ padding: 8, marginBottom: 20 }}
                        />

                        <h3>Hourly Consumption</h3>
                        {chartData.length > 0 ? (
                            <ChartComponent data={chartData} />
                        ) : (
                            <p>No data for this day.</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}