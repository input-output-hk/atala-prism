import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { noop } from 'lodash';
import CustomButton from '../components/customButton/CustomButton';
import { LEFT, RIGHT } from '../helpers/constants';

const helpIcon = {
  src: '/images/icon-help.svg',
  alt: 'HelpSupport'
};

const buttonProps = {
  className: 'theme-primary',
  onClick: noop
};

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<CustomButton />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<CustomButton buttonText="Testing" buttonProps={buttonProps} />);

    expect(screen.getByText('Testing')).toBeTruthy();
  });

  it('renders with img', () => {
    render(<CustomButton buttonText="Testing" img={helpIcon} buttonProps={buttonProps} />);

    expect(screen.getByText('Testing')).toBeTruthy();
    expect(screen.getByAltText(helpIcon.alt)).toBeTruthy();
  });

  it('renders with opt img', () => {
    render(<CustomButton buttonText="Testing" optImg={helpIcon} buttonProps={buttonProps} />);

    expect(screen.getByText('Testing')).toBeTruthy();
    expect(screen.getByAltText(helpIcon.alt)).toBeTruthy();
  });

  it('renders with left icon', () => {
    render(
      <CustomButton
        buttonText="Testing"
        icon={{ side: LEFT, icon: <img alt={helpIcon.alt} src={helpIcon.src} /> }}
        buttonProps={buttonProps}
      />
    );

    expect(screen.getByText('Testing')).toBeTruthy();
    expect(screen.getByAltText(helpIcon.alt)).toBeTruthy();
  });

  it('renders with right icon', () => {
    render(
      <CustomButton
        buttonText="Testing"
        icon={{ side: RIGHT, icon: <img alt={helpIcon.alt} src={helpIcon.src} /> }}
        buttonProps={buttonProps}
      />
    );

    expect(screen.getByText('Testing')).toBeTruthy();
    expect(screen.getByAltText(helpIcon.alt)).toBeTruthy();
  });
});
