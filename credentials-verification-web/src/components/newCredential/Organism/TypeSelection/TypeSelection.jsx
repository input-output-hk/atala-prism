import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import TypeCard from '../../Molecules/TypeCard/TypeCard';

import './_style.scss';

const TypeSelection = ({ credentialTypes, selectedType, onTypeSelection }) => {
  const { t } = useTranslation();

  return (
    <div className="TypeSelectionWrapper">
      <Col className="TypeSelectionContainer">
        <h1>{t('newCredential.typeSelection')}</h1>
        <Row type="flex" align="middle" className="TypeSelection">
          {Object.keys(credentialTypes).map(key => (
            <TypeCard
              credentialType={credentialTypes[key]}
              typeKey={key}
              key={key}
              isSelected={selectedType === key}
              onClick={onTypeSelection}
            />
          ))}
        </Row>
      </Col>
    </div>
  );
};

TypeSelection.defaultProps = {
  selectedType: ''
};

TypeSelection.propTypes = {
  credentialTypes: PropTypes.shape({}).isRequired,
  selectedType: PropTypes.string,
  onTypeSelection: PropTypes.func.isRequired
};

export default TypeSelection;
