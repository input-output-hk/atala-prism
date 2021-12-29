import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import HeaderBlog from '../components/headerBlog/headerBlog';

describe('<HeaderBlog />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<HeaderBlog backTo="/" />);

    expect(screen.getByAltText('atala-prism')).toBeTruthy();
    expect(screen.getByAltText('backBtn')).toBeTruthy();
    expect(screen.getByText('Back')).toBeTruthy();
  });
});
