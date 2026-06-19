import { createContext, useContext, useState } from 'react';

const FavoritesContext = createContext(null);

export function FavoritesProvider({ children }) {
  const [favoriteTeams, setFavoriteTeams] = useState(() => {
    try { return new Set(JSON.parse(localStorage.getItem('favoriteTeams') || '[]')); }
    catch { return new Set(); }
  });
  const [starredMatches, setStarredMatches] = useState(() => {
    try { return new Set(JSON.parse(localStorage.getItem('starredMatches') || '[]')); }
    catch { return new Set(); }
  });

  function toggleTeam(slug) {
    setFavoriteTeams(prev => {
      const next = new Set(prev);
      next.has(slug) ? next.delete(slug) : next.add(slug);
      localStorage.setItem('favoriteTeams', JSON.stringify([...next]));
      return next;
    });
  }

  function toggleMatch(id) {
    setStarredMatches(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      localStorage.setItem('starredMatches', JSON.stringify([...next]));
      return next;
    });
  }

  return (
    <FavoritesContext.Provider value={{
      favoriteTeams, starredMatches,
      toggleTeam, toggleMatch,
      isFavoriteTeam: slug => favoriteTeams.has(slug),
      isStarredMatch: id => starredMatches.has(id),
    }}>
      {children}
    </FavoritesContext.Provider>
  );
}

export function useFavorites() {
  return useContext(FavoritesContext);
}
