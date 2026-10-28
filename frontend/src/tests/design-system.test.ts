import { describe, it, expect } from 'vitest';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';

describe('LaunchCheck Design System Components', () => {
  it('renders Button primary variant correctly', () => {
    const html = renderToStaticMarkup(createElement(Button, { variant: 'primary' }, 'Run Audit'));

    expect(html).toContain('bg-[#38FF8E]');
    expect(html).toContain('Run Audit');
  });

  it('renders Input component with label and monospace support', () => {
    const html = renderToStaticMarkup(createElement(Input, {
      label: 'Target URL',
      monospace: true,
      placeholder: 'https://...'
    }));

    expect(html).toContain('Target URL');
    expect(html).toContain('font-mono');
    expect(html).toContain('https://...');
  });

  it('renders Badge component with PASS status styling', () => {
    const html = renderToStaticMarkup(createElement(Badge, { status: 'PASS', label: 'SYSTEM VERIFIED' }));

    expect(html).toContain('SYSTEM VERIFIED');
    expect(html).toContain('text-[#38FF8E]');
  });
});
