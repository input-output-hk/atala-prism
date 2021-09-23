import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const CustomButton = ({ icon: img, buttonText, buttonProps, optImg }) => {
  const { t } = useTranslation();
  return (
    <Button {...buttonProps}>
      {img.src && <img src={img.src} alt={t(img.alt)} className="CredentialIcon" />}
      {buttonText}
      {optImg.src && <img src={optImg.src} alt={t(optImg.alt)} className="VerifiedIcon" />}
    </Button>
  );
};

CustomButton.propTypes = {
  buttonProps: PropTypes.shape({
    className: PropTypes.oneOf([
      'theme-primary',
      'theme-secondary',
      'theme-outline',
      'theme-grey',
      'theme-link',
      'theme-filter',
      'theme-list'
    ]),
    onClick: PropTypes.func
  }).isRequired,
  buttonText: PropTypes.string,
  icon: PropTypes.shape({ icon: PropTypes.element, side: PropTypes.string }),
  img: PropTypes.shape({ src: PropTypes.string, alt: PropTypes.string }),
  optImg: PropTypes.shape({ src: PropTypes.string, alt: PropTypes.string })
};

CustomButton.defaultProps = {
  img: {},
  optImg: {},
  buttonText: '',
  icon: {}
};

export default CustomButton;
