// frontend/src/components/ChartComponent.jsx
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function ChartComponent({ data }) {
    // data trebuie să fie un array de obiecte: { hour: 10, value: 2.5 }

    // Putem procesa datele primite de la backend pentru a le formata frumos (ex: timestamp -> oră lizibilă)
    const formattedData = data.map(item => {
        const date = new Date(item.timestamp);
        return {
            hour: date.getHours() + ":00", // Axa OX: Ora
            energy: item.totalConsumption  // Axa OY: kWh
        };
    });

    return (
        <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
                <BarChart data={formattedData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="hour" />
                    <YAxis label={{ value: 'Energy (kWh)', angle: -90, position: 'insideLeft' }} />
                    <Tooltip />
                    <Bar dataKey="energy" fill="#8884d8" />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}