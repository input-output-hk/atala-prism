import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const ImportContactsFooter = ({ previous }) => {
  const { t } = useTranslation();

  return (
    <div className="ImportContactsFooter">
      <div className="LeftButtons">
        {previous && (
          <CustomButton
            buttonProps={{
              onClick: previous,
              className: 'theme-grey'
            }}
            buttonText={t('importContacts.backButton')}
          />
        )}
      </div>
    </div>
  );
};

ImportContactsFooter.defaultProps = {
  previous: null
};

ImportContactsFooter.propTypes = {
  previous: PropTypes.func
};

export default ImportContactsFooter;
