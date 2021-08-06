import React from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';
import { useTranslation } from 'react-i18next';
import { Form, Input, message } from 'antd';
import { SEARCH_DELAY_MS } from '../../../../helpers/constants';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { withApi } from '../../../providers/withApi';
import { useCredentialTypes } from '../../../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../../../helpers/propShapes';
import Logger from '../../../../helpers/Logger';

import './_style.scss';

const normalize = input => input.trim();

const TemplateName = ({ api }) => {
  const { t } = useTranslation();
  const { getCredentialTypes } = useCredentialTypes(api.credentialTypesManager);

  const templateExists = async (_rule, value, callback) => {
    try {
      const normalizedValue = normalize(value);
      const credentialTypes = await getCredentialTypes();

      if (exactValueExists(credentialTypes, normalizedValue, 'name')) {
        callback(t('credentialTemplateCreation.errors.preExisting', { value: normalizedValue }));
      } else callback();
    } catch (error) {
      Logger.error('[CredentialTypes.getCredentialTypes] Error: ', error);
      const errorMessage = t('errors.errorGetting', { model: t('templates.model') });
      message.error(errorMessage);
      callback(errorMessage);
    }
  };

  const checkExistence = debounce(templateExists, SEARCH_DELAY_MS);

  return (
    <div className="templateName">
      <Form.Item
        hasFeedback
        className="flex"
        name="name"
        label={t('credentialTemplateCreation.step1.templateName')}
        rules={[{ required: true }, { validator: checkExistence }]}
      >
        <Input placeholder={t('credentialTemplateCreation.step1.templateNamePlaceholder')} />
      </Form.Item>
    </div>
  );
};

TemplateName.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape
  }).isRequired
};

export default withApi(TemplateName);
