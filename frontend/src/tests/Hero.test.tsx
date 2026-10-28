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
  it('starts with the verifiable GitHub targets and passes edited values through', () => {
    const onQuickRun = vi.fn();

    render(<Hero onQuickRun={onQuickRun} onExploreArchitecture={() => {}} />);

    expect(screen.getByDisplayValue('https://github.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('https://api.github.com')).toBeInTheDocument();

    fireEvent.submit(screen.getByRole('button', { name: 'Run a preflight' }).closest('form')!);

    expect(onQuickRun).toHaveBeenCalledWith('https://github.com', 'https://api.github.com');
  });
});
