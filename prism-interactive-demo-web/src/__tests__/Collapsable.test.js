import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import Collapsable, { FAQs } from '../components/collapsable/collapsable';

describe('<Collapsable />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<Collapsable />);

    FAQs.forEach(({ title }) => {
      expect(screen.getByText(title)).toBeTruthy();
    });
  });
});
