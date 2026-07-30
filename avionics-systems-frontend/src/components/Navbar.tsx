import { Link } from 'react-router-dom';

export default function Navbar() {
  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
  };

  return (
    <nav className="bg-gray-900 text-white px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <span className="font-bold text-lg">Systems and Avionics</span>
        <div className="flex gap-4 text-sm">
          <Link to="/dashboard" className="hover:text-gray-300">Dashboard</Link>
          <Link to="/projects" className="hover:text-gray-300">Projects</Link>
          <Link to="/tests" className="hover:text-gray-300">Tests</Link>
          <Link to="/tests/shared-steps" className="hover:text-gray-300">Shared Steps</Link>
          <Link to="/tests/flaky" className="hover:text-gray-300">Flaky Tests</Link>
          <Link to="/boards" className="hover:text-gray-300">Boards</Link>
          <Link to="/sprints" className="hover:text-gray-300">Sprints</Link>
          <Link to="/workflows" className="hover:text-gray-300">Workflows</Link>
          <Link to="/search" className="hover:text-gray-300">Search</Link>
          <Link to="/migration" className="hover:text-gray-300">Migration</Link>
          <Link to="/audit" className="hover:text-gray-300">Audit</Link>
          <Link to="/admin" className="hover:text-gray-300">Admin</Link>
        </div>
      </div>
      <button onClick={handleLogout} className="text-sm hover:text-gray-300">
        Logout
      </button>
    </nav>
  );
}
