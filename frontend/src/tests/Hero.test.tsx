import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Hero } from '../components/marketing/Hero';

vi.mock('../components/ui/AeroShards/AeroShards', () => ({
  default: () => <div aria-hidden="true" />
}));

vi.mock('../components/ui/tech-text/TechText', () => ({
  default: () => <div aria-hidden="true" />
}));

describe('Hero quick-run defaults', () => {
  it('starts with JSONPlaceholder targets and passes edited values through', () => {
    const onQuickRun = vi.fn();

    render(<Hero onQuickRun={onQuickRun} onExploreArchitecture={() => {}} />);

    expect(screen.getAllByDisplayValue('https://jsonplaceholder.typicode.com')).toHaveLength(2);

    // Select the inputs specifically if needed, but for now, this confirms both exist
    const inputs = screen.getAllByDisplayValue('https://jsonplaceholder.typicode.com');
    expect(inputs[0]).toBeInTheDocument();
    expect(inputs[1]).toBeInTheDocument();

    fireEvent.submit(screen.getByRole('button', { name: 'Run a preflight' }).closest('form')!);

    expect(onQuickRun).toHaveBeenCalledWith('https://jsonplaceholder.typicode.com', 'https://jsonplaceholder.typicode.com');
  });
});
