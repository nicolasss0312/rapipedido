import { Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import Inicio from './vistas/Inicio';
import Cliente from './vistas/Cliente';
import PanelRestaurante from './vistas/Restaurante';
import AppRepartidor from './vistas/Repartidor';
import PanelAdmin from './vistas/Admin';

export default function App() {
  return (
    <div className="app">
      <header className="barra">
        <Link to="/" className="marca">RapiPedido</Link>
        <nav className="roles">
          <NavLink to="/cliente">Cliente</NavLink>
          <NavLink to="/restaurante">Restaurante</NavLink>
          <NavLink to="/repartidor">Repartidor</NavLink>
          <NavLink to="/admin">Administración</NavLink>
        </nav>
      </header>
      <main className="contenido">
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/cliente/*" element={<Cliente />} />
          <Route path="/restaurante/*" element={<PanelRestaurante />} />
          <Route path="/repartidor" element={<AppRepartidor />} />
          <Route path="/admin" element={<PanelAdmin />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
