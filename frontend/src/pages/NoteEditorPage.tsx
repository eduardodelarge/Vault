import { type FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as notesApi from '../api/notesApi';

export function NoteEditorPage() {
  const { id } = useParams<{ id: string }>();
  const isNew = id === undefined;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data: existingNote, isLoading } = useQuery({
    queryKey: ['note', id],
    queryFn: () => notesApi.getNote(id!),
    enabled: !isNew,
  });

  useEffect(() => {
    if (existingNote) {
      setTitle(existingNote.title);
      setContent(existingNote.content);
    }
  }, [existingNote]);

  const saveMutation = useMutation({
    mutationFn: () =>
      isNew ? notesApi.createNote(title, content) : notesApi.updateNote(id!, title, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes'] });
      navigate('/');
    },
    onError: () => setError('Nao foi possivel salvar a nota.'),
  });

  const deleteMutation = useMutation({
    mutationFn: () => notesApi.deleteNote(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes'] });
      navigate('/');
    },
    onError: () => setError('Nao foi possivel excluir a nota.'),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    saveMutation.mutate();
  }

  function handleDelete() {
    if (confirm('Excluir esta nota? Essa acao nao pode ser desfeita.')) {
      deleteMutation.mutate();
    }
  }

  if (!isNew && isLoading) {
    return (
      <div className="page">
        <p>Carregando...</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>{isNew ? 'Nova nota' : 'Editar nota'}</h1>

      {error && <div className="form-error">{error}</div>}

      <form className="note-form" onSubmit={handleSubmit}>
        <label>
          Titulo
          <input value={title} onChange={(e) => setTitle(e.target.value)} required autoFocus />
        </label>

        <label>
          Conteudo
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={12} required />
        </label>

        <div className="note-form-actions">
          <button type="submit" disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Salvando...' : 'Salvar'}
          </button>
          <button type="button" className="button-secondary" onClick={() => navigate('/')}>
            Cancelar
          </button>
          {!isNew && (
            <button
              type="button"
              className="button-danger"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
            >
              Excluir
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
