import React from 'react';
import { useAnalytics } from 'reactfire';
import CustomButton from '../CustomButton/CustomButton';
import { SUPPORT_EVENT } from '../../../../helpers/constants';
import './_style.scss';

const helpIcon = {
  src: '/images/icon-help.svg',
  alt: 'HelpSupport'
};

const SupportButton = () => {
  const firebase = useAnalytics();

  return (
    <div className="SupportButton">
      <a
        href="https://iohk.zendesk.com/hc/en-us/requests/new"
        target="_blank"
        rel="noopener noreferrer"
      >
        <CustomButton
          buttonProps={{
            className: 'theme-primary',
            onClick: () => firebase.logEvent(SUPPORT_EVENT)
          }}
          img={helpIcon}
        />
      </a>
    </div>
  );
};

export default SupportButton;
