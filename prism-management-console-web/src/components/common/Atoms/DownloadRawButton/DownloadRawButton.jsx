import React from 'react';
import PropTypes from 'prop-types';
import { Tooltip } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../CustomButton/CustomButton';

const DownloadRawButton = ({
  downloadHref,
  downloadName,
  disabled,
  helpText,
  overrideClassName
}) => {
  const { t } = useTranslation();
  return (
    <Tooltip title={helpText}>
      <a href={downloadHref} download={downloadName}>
        <CustomButton
          overrideClassName={overrideClassName}
          buttonProps={{
            className: 'theme-link',
            disabled
          }}
          buttonText={t('credentials.drawer.raw.downloadFile')}
        />
      </a>
    </Tooltip>
  );
};

DownloadRawButton.defaultProps = {
  disabled: false,
  helpText: '',
  overrideClassName: ''
};

DownloadRawButton.propTypes = {
  downloadHref: PropTypes.string.isRequired,
  downloadName: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
  helpText: PropTypes.string,
  overrideClassName: PropTypes.string
};

export default DownloadRawButton;
