import React from 'react';
import ReactDOM from 'react-dom/client';
import './assets/css/common.css';
import './assets/css/variables.css';
import './index.css';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import MainPage from './pages/MainPage.tsx';
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { persistor, store } from './util/store.ts';
import GoogleLoginPage from './pages/GoogleLoginPage.tsx';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
            <BrowserRouter>
                <Routes>
                    <Route index element={<MainPage />}></Route>
                    <Route
                        path="/login/oauth2/code/google"
                        element={<GoogleLoginPage />}
                    ></Route>
                </Routes>
            </BrowserRouter>
        </PersistGate>
    </Provider>
);
