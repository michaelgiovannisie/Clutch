import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { SettingsProvider } from './context/SettingsContext.jsx';
import { FavoritesProvider } from './context/FavoritesContext.jsx';
import './styles/theme.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <SettingsProvider>
        <FavoritesProvider>
          <App />
        </FavoritesProvider>
      </SettingsProvider>
    </BrowserRouter>
  </React.StrictMode>
);
