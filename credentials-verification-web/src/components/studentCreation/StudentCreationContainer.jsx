import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import moment from 'moment';
import StudentCreation from './StudentCreation';
import { withApi } from '../providers/withApi';
import { withRedirector } from '../providers/withRedirector';
import Logger from '../../helpers/Logger';
import { fromMomentToProtoDateFormatter } from '../../helpers/formatters';

const createBlankStudent = key => ({
  key,
  fullName: '',
  email: '',
  studentId: '',
  admissionDate: ''
});
const defaultStudentList = [createBlankStudent(0)];

const StudentCreationContainer = ({ api, redirector: { redirectToConnections } }) => {
  const { t } = useTranslation();
  const [students, setStudents] = useState(defaultStudentList);
  const [invalidFields, setInvalidFields] = useState([]);
  const [groups, setGroups] = useState([]);
  const [group, setGroup] = useState();

  useEffect(() => {
    api.groupsManager
      .getGroups()
      .then(response => {
        const [{ name }] = response;

        setGroups(response);
        setGroup(name);
      })
      .catch(error => {
        Logger.error('[GroupsContainer.updateGroups] Error: ', error);
        message.error(t('errors.errorGetting', { model: t('groups.title') }));
      });
  }, []);

  const isInvalidValueByKey = (key, value) => {
    switch (key) {
      case 'admissionDate':
        return !(value && moment(value).isSameOrBefore(moment()));
      case 'key':
        return false;
      default:
        return !value;
    }
  };

  const validateStudent = (student, invalidFieldsObject) => {
    const updatedFieldsObject = Object.assign({}, invalidFieldsObject);

    Object.keys(student).forEach(key => {
      const invalidKey = isInvalidValueByKey(key, student[key]);
      if (invalidKey && !invalidFieldsObject[key]) updatedFieldsObject[key] = key;
    });

    return updatedFieldsObject;
  };

  const getInvalidFields = () => {
    let invalidFieldsObject = {};

    students.forEach(currentStudent => {
      invalidFieldsObject = validateStudent(currentStudent, invalidFieldsObject);
    });

    return Object.keys(invalidFieldsObject);
  };

  const addNewStudent = () => {
    const { key = 0 } = _.last(students) || {};

    const newData = createBlankStudent(key + 1);
    const newDataSource = students.concat(newData);

    setStudents(newDataSource);
  };

  const editStudent = student => {
    const clonedStudents = [...students];

    const index = clonedStudents.findIndex(({ key }) => student.key === key);
    const item = clonedStudents[index];
    const editedStudent = Object.assign({}, item, student);

    clonedStudents.splice(index, 1, editedStudent);

    setStudents(clonedStudents);
  };

  const deleteStudent = key => {
    const filteredStudents = students.filter(({ key: studentKey }) => key !== studentKey);

    setStudents(filteredStudents);
  };

  const saveStudents = () => {
    if (!students.length) return redirectToConnections();

    const invalidFieldos = getInvalidFields();

    setInvalidFields(invalidFieldos);

    if (invalidFieldos.length) return;

    const creationPromises = students.map(({ fullName, email, studentId, admissionDate }) =>
      api.studentsManager.createStudent({
        studentId,
        fullName,
        email,
        admissionDate: fromMomentToProtoDateFormatter(admissionDate),
        groupName: group
      })
    );

    Promise.all(creationPromises)
      .then(() => {
        message.success(t('studentCreation.success'));
        redirectToConnections();
      })
      .catch(error => {
        Logger.error('Error while creating student', error);
        message.error('Error while saving the student');
      });
  };

  const tableProps = {
    students,
    deleteStudent,
    editStudent
  };

  return (
    <StudentCreation
      saveStudent={addNewStudent}
      saveStudents={saveStudents}
      tableProps={tableProps}
      invalidFields={invalidFields}
      groups={groups}
      group={group}
      setGroup={setGroup}
      canAddMore={getInvalidFields().length}
    />
  );
};

StudentCreationContainer.propTypes = {
  api: PropTypes.shape({
    studentsManager: PropTypes.shape({ createStudent: PropTypes.func }).isRequired,
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToConnections: PropTypes.func }).isRequired
};

export default withApi(withRedirector(StudentCreationContainer));
