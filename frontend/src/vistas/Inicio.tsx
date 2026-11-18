import { Link } from 'react-router-dom';

const ROLES = [
  { ruta: '/cliente', titulo: 'Clientes', texto: 'Buscan restaurantes, arman el carrito, pagan en línea y siguen el pedido en vivo.' },
  { ruta: '/restaurante', titulo: 'Restaurantes', texto: 'Reciben pedidos, gestionan el menú y avisan cuando está listo.' },
  { ruta: '/repartidor', titulo: 'Repartidores', texto: 'Aceptan entregas, comparten su ubicación cada 5 s y confirman la entrega.' },
  { ruta: '/admin', titulo: 'Administración', texto: 'Reportes, estado de la pasarela de pagos y de los repartidores.' },
];

export default function Inicio() {
  return (
    <div className="inicio">
      <section className="portada">
        <p className="sobretitulo">Plataforma de domicilios</p>
        <h1>RapiPedido</h1>
        <p>Elige con qué actor quieres entrar. Abre cada uno en una pestaña distinta para ver cómo se mueve un pedido entre todos.</p>
      </section>
      <div className="rejilla-roles">
        {ROLES.map((rol) => (
          <Link key={rol.ruta} to={rol.ruta} className="tarjeta tarjeta-rol">
            <h2>{rol.titulo}</h2>
            <p>{rol.texto}</p>
            <span className="ir">Entrar →</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
