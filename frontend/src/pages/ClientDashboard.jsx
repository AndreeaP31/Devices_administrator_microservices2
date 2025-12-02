import { useEffect, useState } from "react";
import { getDevicesForUser } from "../api";

export default function ClientDashboard({ user }) {
    const [devices, setDevices] = useState([]);

    useEffect(() => {
        async function load() {
            const result = await getDevicesForUser(user.userId);
            setDevices(result);
        }

        load();
    }, [user.userId]); // dependency corectă

    return (
        <div className="card">
            <h2>Your Devices</h2>

            {devices.length === 0 && (
                <div className="muted">No devices assigned yet.</div>
            )}

            {devices.map((d) => (
                <div className="list-row" key={d.id}>
                    <strong>{d.name}</strong>
                    <div className="muted">Max: {d.maxCons}</div>
                </div>
            ))}
        </div>
    );
}
