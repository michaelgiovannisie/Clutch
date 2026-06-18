import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import Home from './pages/Home.jsx';
import Teams from './pages/Teams.jsx';
import TeamDetail from './pages/TeamDetail.jsx';
import Matches from './pages/Matches.jsx';
import MatchDetail from './pages/MatchDetail.jsx';
import Venues from './pages/Venues.jsx';
import VenueDetail from './pages/VenueDetail.jsx';
import Standings from './pages/Standings.jsx';
import DesignReference from './pages/DesignReference.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="teams" element={<Teams />} />
        <Route path="teams/:slug" element={<TeamDetail />} />
        <Route path="matches" element={<Matches />} />
        <Route path="matches/:id" element={<MatchDetail />} />
        <Route path="venues" element={<Venues />} />
        <Route path="venues/:id" element={<VenueDetail />} />
        <Route path="standings" element={<Standings />} />
        <Route path="design" element={<DesignReference />} />
        <Route path="*" element={<Home />} />
      </Route>
    </Routes>
  );
}
