import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Button } from '../components/ui/Button';

describe('Button Component', () => {
  it('renders with primary variant styles', () => {
    render(<Button variant="primary">Click Me</Button>);
    const button = screen.getByRole('button', { name: /click me/i });

    expect(button).toBeDefined();
    expect(button.className).toContain('bg-[#38FF8E]');
  });
});
