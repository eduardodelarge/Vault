import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchNotes } from '../api/notesApi';
import { NoteCard } from '../components/NoteCard';
import { SearchBar } from '../components/SearchBar';

export function DashboardPage() {
  const [query, setQuery] = useState('');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['notes', query],
    queryFn: () => searchNotes(query),
  });

  return (
    <div className="page">
      <div className="page-header">
        <h1>Minhas notas</h1>
        <Link to="/notes/new" className="button-primary">
          + Nova nota
        </Link>
      </div>

      <SearchBar value={query} onChange={setQuery} />

      {isLoading && <p>Carregando...</p>}
      {isError && <p className="form-error">Nao foi possivel carregar as notas.</p>}

      {data && data.content.length === 0 && (
        <p className="empty-state">
          {query ? 'Nenhuma nota encontrada para essa busca.' : 'Voce ainda nao tem notas. Crie a primeira!'}
        </p>
      )}

      <div className="note-grid">
        {data?.content.map((note) => (
          <NoteCard key={note.id} note={note} />
        ))}
      </div>
    </div>
  );
}
