import React, { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

const fetchApi = async (endpoint, method = 'GET', body = null) => {
  try {
    const options = {
      method,
      headers: { 'Content-Type': 'application/json' },
    };
    if (body) options.body = JSON.stringify(body);

    const response = await fetch(`${BACKEND_URL}${endpoint}`, options);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const text = await response.text();

    if (!text) return { success: true };

    try {
      return JSON.parse(text);
    } catch (parseError) {
      return { message: text };
    }

  } catch (error) {
    console.error(`Error with ${endpoint}:`, error);
    return { error: error.message };
  }
};

function App() {
  const [marketUpdates, setMarketUpdates] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [wsStatus, setWsStatus] = useState('Connecting...');

  const [userId, setUserId] = useState('');
  const [portfolioId, setPortfolioId] = useState('');
  const [ticker, setTicker] = useState('');

  const [portfoliosRes, setPortfoliosRes] = useState(null);
  const [holdingsRes, setHoldingsRes] = useState(null);
  const [marketRes, setMarketRes] = useState(null);

  useEffect(() => {
    const marketClient = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      reconnectDelay: 5000,
      onConnect: () => {
        setWsStatus('Connected');
        marketClient.subscribe('/topic/prices', (message) => {
          const newData = JSON.parse(message.body);
          setMarketUpdates(prev => [newData, ...prev].slice(0, 5));
        });
      },
      onDisconnect: () => setWsStatus('Disconnected'),
      onWebSocketError: (error) => console.error('Market WS Error:', error),
    });

    const alertsClient = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-alerts`),
      reconnectDelay: 5000,
      onConnect: () => {
        alertsClient.subscribe('/topic/alerts', (message) => {
          const newAlert = JSON.parse(message.body);
          setAlerts(prev => [newAlert, ...prev].slice(0, 5));
        });
      },
    });

    marketClient.activate();
    alertsClient.activate();

    return () => {
      marketClient.deactivate();
      alertsClient.deactivate();
    };
  }, []);

  const handleGetPortfolios = async () => {
    const data = await fetchApi(`/users/${userId}/portfolios`);
    setPortfoliosRes(data);
  };

  const handleGetHoldings = async () => {
    const data = await fetchApi(`/portfolios/${portfolioId}/holdings`);
    setHoldingsRes(data);
  };

  const handleGetMarketProfile = async () => {
    const data = await fetchApi(`/market/profiles/${ticker}`);
    setMarketRes(data);
  };

  const handleTrackStock = async () => {
    const data = await fetchApi(`/market/tracked-stocks/${ticker}`, 'POST');
    setMarketRes(data);
  };

  const handleUntrackStock = async () => {
    const data = await fetchApi(`/market/tracked-stocks/${ticker}`, 'DELETE');
    setMarketRes(data);
  };

  const sectionStyle = { border: '1px solid #ccc', padding: '15px', marginBottom: '20px', borderRadius: '5px' };
  const preStyle = { background: '#f4f4f4', padding: '10px', maxHeight: '200px', overflowY: 'auto' };
  const buttonStyle = { marginLeft: '10px', padding: '4px 8px', cursor: 'pointer', border: '1px solid #ccc', borderRadius: '4px' };

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif', maxWidth: '1200px', margin: '0 auto' }}>
      <h1>Backend API Tester</h1>
      <p>WebSocket Status: <strong>{wsStatus}</strong></p>
      <hr />

      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>

        <div style={{ flex: '1 1 400px' }}>
          <h2>HTTP Endpoints</h2>

          <div style={sectionStyle}>
            <h3>User Portfolios</h3>
            <div>
              <label>User ID: </label>
              <input value={userId} onChange={(e) => setUserId(e.target.value)} />
              <button onClick={handleGetPortfolios} style={buttonStyle}>GET Portfolios</button>
            </div>
            <pre style={preStyle}>{JSON.stringify(portfoliosRes, null, 2) || 'No data yet'}</pre>
          </div>

          <div style={sectionStyle}>
            <h3>Portfolio Holdings</h3>
            <div>
              <label>Portfolio ID: </label>
              <input value={portfolioId} onChange={(e) => setPortfolioId(e.target.value)} />
              <button onClick={handleGetHoldings} style={buttonStyle}>GET Holdings</button>
            </div>
            <pre style={preStyle}>{JSON.stringify(holdingsRes, null, 2) || 'No data yet'}</pre>
          </div>

          <div style={sectionStyle}>
            <h3>Market Profile & Tracking</h3>
            <div>
              <label>Ticker: </label>
              <input value={ticker} onChange={(e) => setTicker(e.target.value)} />
              <button onClick={handleGetMarketProfile} style={buttonStyle}>GET Profile</button>
              <button onClick={handleTrackStock} style={{ ...buttonStyle, backgroundColor: '#4caf50', color: 'white', borderColor: '#4caf50' }}>Add to Tracking</button>
              <button onClick={handleUntrackStock} style={{ ...buttonStyle, backgroundColor: '#f44336', color: 'white', borderColor: '#f44336' }}>Remove from Tracking</button>
            </div>
            <pre style={preStyle}>{JSON.stringify(marketRes, null, 2) || 'No data yet'}</pre>
          </div>
        </div>

        <div style={{ flex: '1 1 400px' }}>
          <h2>Live WebSocket Data</h2>

          <div style={sectionStyle}>
            <h3>Market Prices (/topic/prices)</h3>
            <pre style={{ ...preStyle, background: '#e6ffe6' }}>
              {marketUpdates.length > 0 ? JSON.stringify(marketUpdates, null, 2) : 'Waiting for prices...'}
            </pre>
          </div>

          <div style={sectionStyle}>
            <h3>Price Alerts (/topic/alerts)</h3>
            <pre style={{ ...preStyle, background: '#ffe6e6' }}>
              {alerts.length > 0 ? JSON.stringify(alerts, null, 2) : 'Waiting for alerts...'}
            </pre>
          </div>
        </div>

      </div>
    </div>
  );
}

export default App;