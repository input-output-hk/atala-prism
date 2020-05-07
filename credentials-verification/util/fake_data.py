import io
import random
import traceback
import coincurve

from sys import argv
from faker import Faker
from datetime import datetime, timedelta, date
from random import randint
from PIL import Image, ImageDraw, ImageFont
from math import sin, pi
from hashlib import sha256

from credential_pb2 import *

# whether data is generated for demo - less data to be generated
is_demo = False

font_path="/usr/share/fonts/dejavu/DejaVuSansCondensed.ttf"
try:
    font = ImageFont.truetype(font_path, 200)
except Exception as ex:
    font = None
    traceback.print_exc()
    print("Couldn't open font, icons won't be rendered (you can replace the path in the script)")
    print("Test data will still be generated")

def random_key():
    sk = coincurve.PrivateKey()
    #sk = coincurve.PrivateKey(secret = str(randint(1,1000)).encode('utf8'))
    pk = sk.public_key
    return {
            'curve': 'P-256K',
            'x': pk.point()[0],
            'y': pk.point()[1]
            }

def random_color():
    phi = randint(0, 1000)
    (sin(phi)**2, sin(phi + 2*pi/3)**2, sin(phi + 4*pi/3)**2)

def random_icon(two_letters):
    if font:
        img = Image.new('RGB', (300, 300), color = (73, 109, 137))
        d = ImageDraw.Draw(img)
        d.text((30, 30), two_letters, font=font, fill=(0,0,0))
        bytes_io = io.BytesIO()
        img.save(bytes_io, format='PNG')
        open('icon.png', 'wb').write(bytes_io.getvalue())
        return bytes_io.getvalue()
    else:
        return None

CREATED = 'CREATED'
INVITED = 'INVITED'
CONNECTED = 'CONNECTED'
REVOKED = 'REVOKED'

student_statuses = {
        CREATED: 'INVITATION_MISSING',
        INVITED: 'CONNECTION_MISSING',
        CONNECTED: 'CONNECTION_ACCEPTED',
        REVOKED: 'CONNECTION_REVOKED'
        }

states = [CREATED, INVITED, CONNECTED, REVOKED]

def random_state():
    r = randint(1, 100)
    if r <= 20:
        return CREATED
    elif r <= 40:
        return INVITED
    elif is_demo or r <= 95:
        return CONNECTED
    else:
        return REVOKED

def later_state(s1, s2):
    return states.index(s1) >= states.index(s2)

def random_holder(fake):
    if randint(0, 1) == 0:
        name = fake.name_male()
        surname = fake.last_name_male()
    else:
        name = fake.name_female()
        surname = fake.last_name_female()

    full_name = "{} {}".format(name, surname)
    birth = fake.date_time_ad(start_datetime=datetime(1990, 1, 1), end_datetime=datetime(1995, 1, 1))

    doc_id = None
    try:
        doc_id = fake.ein()
    except:
        doc_id = fake.ssn().replace("-", "")

    return {
            'name': name,
            'surname': surname,
            'full_name': full_name,
            'email': fake.email(),
            'birth_at': birth,
            'birth_date': birth.date(),
            'document_id': doc_id,
            'key': random_key(),
            'fake': fake
            }

def test_holder(full_name, email, country):
    fake = Faker(country)
    birth = fake.date_time_ad(start_datetime=datetime(1990, 1, 1), end_datetime=datetime(1995, 1, 1))

    name, surname = full_name.split()

    doc_id = None
    try:
        doc_id = fake.ein()
    except:
        doc_id = fake.ssn().replace("-", "")

    return {
            'name': name,
            'surname': surname,
            'full_name': full_name,
            'email': email,
            'birth_at': birth,
            'birth_date': birth.date(),
            'document_id': doc_id,
            'key': random_key(),
            'state': CREATED,
            'fake': fake
            }


def random_student(fake, holder, issuer):
    admission = holder['birth_at'] + timedelta(365 * randint(18, 21) + randint(0, 365))
    graduation = admission + timedelta(365 * randint(3, 5) + randint(0, 6), randint(0, 3600*24))
    creation = graduation + timedelta(randint(0, 6), randint(0, 3600*24))
    connection_status = holder.get('state', None) or random_state()

    connection = None
    connection_token = None
    connection_id = None
    connector_id = None

    if later_state(connection_status, INVITED):
        connection_token = fake.ean13()

    if later_state(connection_status, CONNECTED):
        connection = creation + timedelta(randint(0, 6), randint(0, 3600*24))
        connection_id = fake.uuid4(cast_to=str)
        connector_id = fake.uuid4(cast_to=str)

    student_data = {
            'student_id': fake.uuid4(cast_to=str),
            'connector_id': connector_id,
            'issuer_id': issuer['issuer_id'],
            'group_id': random.choice(issuer['groups'])['group_id'],
            'university_assigned_id': fake.ean8(),
            'names': [holder['name']],
            'surnames': [holder['surname']],
            'email': fake.email(),
            'admission_date': admission.date(),
            'graduation_date': graduation.date(),
            'created_at': creation,
            'connected_at': connection,
            'connection_status': connection_status,
            'connection_token': connection_token,
            'connection_id': connection_id
            }
    student_data.update(holder)
    return student_data

def random_individual(fake, holder, verifier):
    creation = holder['birth_at'] + timedelta(365 * randint(24, 28) + randint(0, 365))

    connection_status = holder.get('state', None) or random_state()

    connection = None
    connection_token = None
    connection_id = None
    connector_id = None

    if later_state(connection_status, INVITED):
        connection_token = fake.ean13()

    if later_state(connection_status, CONNECTED):
        connection = creation + timedelta(randint(0, 6), randint(0, 3600*24))
        connection_id = fake.uuid4(cast_to=str)
        connector_id = fake.uuid4(cast_to=str)

    individual_data = {
            'verifier': verifier,
            'verifier_id': verifier['verifier_id'],
            'individual_id': fake.uuid4(cast_to=str),
            'connection_status': connection_status,
            'connection_token': connection_token,
            'connection_id': connection_id,
            'created_at': creation,
            'connected_at': connection,
            'connector_id': connector_id
            }
    individual_data.update(holder)
    return individual_data

def sql(d):
    if d is None:
        return "NULL"
    elif isinstance(d, str):
        return "'{}'".format(d)
    elif isinstance(d, bytes):
        return """E'\\\\x{}'""".format(d.hex())
    elif isinstance(d, int):
        return str(d)
    elif isinstance(d, datetime):
        return "'{}'".format(d.isoformat())
    elif isinstance(d, date):
        return "'{}'".format(d.isoformat())
    elif isinstance(d, dict):
        return { k: sql(d[k]) for k in d}
    else:
        return '???{}???'.format(repr(d))

def random_credential(fake, issuer, student):
    creation = student['created_at'] + timedelta(randint(0, 6), randint(0, 3600*24))
    issuance = creation + timedelta(randint(0, 6), randint(0, 3600*24))
    delivery = issuance + timedelta(0, randint(0, 3600*24))
    return {
            'credential_id': fake.uuid4(str),
            'issuer': issuer,
            'issuer_id': issuer['issuer_id'],
            'student': student,
            'student_id': student['student_id'],
            'title': 'Bachelor of Science',
            'issue_number': str(randint(500000, 999999)),
            'registration_number': str(randint(300000, 500000)),
            'decision_number': str(randint(100000, 300000)),
            'enrollment_date': student['admission_date'],
            'graduation_date': student['graduation_date'],
            'group_name': [grp for grp in issuer['groups'] if grp['group_id'] == student['group_id']][0]['name'],
            'created_at': creation,
            'issued_on': issuance.date(),
            'delivered_at': delivery
            }

def proto_issuer(d):
    return IssuerData(
            issuerLegalName = d['legal_name'],
            academicAuthority = d['academic_authority'],
            did = d['did']
            )
def proto_subject(d):
    return SubjectData(
            names = d['names'],
            surnames = d['surnames'],
            dateOfBirth = proto_date(d['birth_date']),
            idDocument = PersonalId(id = d['document_id'], documentType = IdDocumentType.Value('NationalIdCard'))
            )

def proto_signer(d):
    return Signer(
            names = d['names'],
            surnames = d['surnames'],
            role = d['role'],
            did = d['did'],
            title = d['title']
            )

def proto_date(d):
    return Date(year = d.year, month = d.month, day = d.day)

def proto_credential(d):
    return Credential(
            issuerType = proto_issuer(d['issuer']),
            subjectData = proto_subject(d['student']),
            grantingDecision = "",
            signingAuthorities = [proto_signer(d['issuer']['signer'])],
            degreeAwarded = "",
            issuedOn = proto_date(d['issued_on']),
            issueNumber = d['issue_number'],
            registrationNumber = d['registration_number'],
            decisionNumber = d['decision_number'],
            yearCompletedByStudent = '3',
            admissionDate = proto_date(d['student']['admission_date']),
            graduationDate = proto_date(d['graduation_date']),
            attainmentDate = proto_date(d['issued_on'])
            )

def render_issuer(d):
    return '''INSERT INTO issuers(issuer_id, did, name)
  VALUES({issuer_id}, {did}, {name});'''.format(**sql(d))

def render_group(g):
    return '''INSERT INTO issuer_groups (group_id, issuer_id, name)
  VALUES({group_id}, {issuer_id}, {name});'''.format(**sql(g))

def render_issuer_participant(d):
    return '''INSERT INTO participants(id, tpe, did, name, logo)
  VALUES({issuer_id}, 'issuer', {did}, {name}, {logo});'''.format(**sql(d))

def render_verifier(d):
    return '''INSERT INTO verifiers(user_id) VALUES({verifier_id});'''.format(**sql(d))

def render_verifier_participant(d):
    return '''INSERT INTO participants(id, tpe, did, name, logo)
  VALUES({verifier_id}, 'verifier', NULL, {name}, {logo});'''.format(**sql(d))

def render_student(d):
    dd = d.copy()
    dd['connection_status'] = student_statuses[d['connection_status']]
    return '''INSERT INTO issuer_subjects (student_id, group_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id)
  VALUES ({student_id}, {group_id}, {university_assigned_id}, {full_name}, {email}, {admission_date}, {created_at}, {connection_status}, {connection_token}, {connection_id});'''.format(**sql(dd))

def render_individual(d):
    return '''INSERT INTO verifier_holders (user_id, individual_id, status, connection_token, connection_id, full_name, email, created_at)
  VALUES ({verifier_id}, {individual_id}, {connection_status}, {connection_token}, {connection_id}, {full_name}, {email}, {created_at});'''.format(**sql(d))

def render_holder_participant(d):
    return '''INSERT INTO participants(id, tpe, did, name)
  VALUES({connector_id}, 'holder', NULL, {full_name});'''.format(**sql(d))

def render_holder_public_key(d):
    dd = {'x': d['key']['x'], 'y': d['key']['y'], 'connector_id': d['connector_id']}
    return '''INSERT INTO holder_public_keys(participant_id, x, y) VALUES ({connector_id}, {x}, {y});'''.format(**sql(dd))

def render_student_issuer_connection(d):
    sqls = []
    if later_state(d['connection_status'], INVITED):
        sqls.append('''INSERT INTO connection_tokens(token, initiator, created_at, used_at)
  VALUES({connection_token}, {issuer_id}, {created_at}, {connected_at});'''.format(**sql(d)))

    if later_state(d['connection_status'], CONNECTED):
        sqls.append('''INSERT INTO connections(id, initiator, acceptor, token, instantiated_at)
  VALUES({connection_id}, {issuer_id}, {connector_id}, {connection_token}, {connected_at});'''.format(**sql(d)))

    return '\n'.join(sqls)

def render_individual_verifier_connection(d):
    sqls = []
    if later_state(d['connection_status'], INVITED):
        sqls.append('''INSERT INTO connection_tokens(token, initiator, created_at, used_at)
  VALUES({connection_token}, {verifier_id}, {created_at}, {connected_at});'''.format(**sql(d)))

    if later_state(d['connection_status'], CONNECTED):
        sqls.append('''INSERT INTO connections(id, initiator, acceptor, token, instantiated_at)
  VALUES({connection_id}, {verifier_id}, {connector_id}, {connection_token}, {connected_at});'''.format(**sql(d)))

    return '\n'.join(sqls)


def render_credential(d):
    return '''INSERT INTO credentials(credential_id, issuer_id, student_id, title, enrollment_date, graduation_date, group_name, created_on)
  VALUES ({credential_id}, {issuer_id}, {student_id}, {title}, {enrollment_date}, {graduation_date}, {group_name}, {created_at});'''.format(**sql(d))

def render_credential_issuer_message(d):
    message_id = fake_en.uuid4(cast_to=str)
    content = IssuerSentCredential(credential = proto_credential(d)).SerializeToString()
    dd = {
            'id': message_id,
            'received_at': d['delivered_at'],
            'connection': d['student']['connection_id'],
            'recipient': d['student']['connector_id'],
            'sender': d['issuer_id'],
            'content': content
            }
    return """INSERT INTO messages (id, received_at, connection, recipient, sender, content)
  VALUES ({id}, {received_at}, {connection}, {recipient}, {sender}, {content});""".format(**sql(dd))

def render_credential_verifier_message(d, ind):
    message_id = fake_en.uuid4(cast_to=str)
    content = HolderSentCredential(credential = proto_credential(d)).SerializeToString()
    received = max(ind['connected_at'], d['delivered_at']) + timedelta(randint(3600, 48*3600))

    dd = {
            'id': message_id,
            'received_at': received,
            'connection': ind['connection_id'],
            'recipient': ind['connector_id'],
            'sender': ind['verifier_id'],
            'content': content
            }
    return """INSERT INTO messages (id, received_at, connection, recipient, sender, content)
  VALUES ({id}, {received_at}, {connection}, {recipient}, {sender}, {content});""".format(**sql(dd))

def print_issuer(issuer, out = sys.stdout):
    print(render_issuer(issuer), file = out)
    for group in issuer['groups']:
        print(render_group({'issuer_id': issuer['issuer_id'], **group}), file = out)
    print(render_issuer_participant(issuer), file = out)
    print(file = out)

def print_verifier(verifier, out = sys.stdout):
    print(render_verifier(verifier), file = out)
    print(render_verifier_participant(verifier), file = out)
    print(file = out)

def print_student(student, out = sys.stdout):
    print(render_student(student), file = out)
    if later_state(student['connection_status'], CONNECTED):
        print(render_holder_participant(student), file = out)
        print(render_holder_public_key(student), file = out)
    print(render_student_issuer_connection(student), file = out)
    print(file = out)

def print_individual(individual, out = sys.stdout):
    print(render_individual(individual), file = out)
    if later_state(individual['connection_status'], CONNECTED):
        print(render_holder_participant(individual), file = out)
        print(render_holder_public_key(individual), file = out)
    print(render_individual_verifier_connection(individual), file = out)
    print(file = out)

def print_credential(credential, send = True, out = sys.stdout):
    print(render_credential(credential), file = out)
    if send:
        print(render_credential_issuer_message(credential), file = out)
    print(file = out)

def print_verifier_sent_credential(credential, individual, out = sys.stdout):
    print(render_credential_verifier_message(credential, individual), file = out)
    print(file = out)

fake_en = Faker('en_US')
fake_ge = Faker('ka_GE')

main_issuer = {
        'issuer_id': 'ebe65434-3015-4ea4-97c8-1f4181312bcf',
        'did': 'did:atala:a80e87d038bce6ba6f184e28a8a35244021802b7f1971f8e2e6d97c55188190f',
        'groups': [{'group_id': fake_en.uuid4(cast_to=str), 'name': 'Bachelors Degree Mathematics and Science 2019'},
                   {'group_id': fake_en.uuid4(cast_to=str), 'name': 'Masters Degree Business Administration 2019'}],
        'name': 'Free University of Tbilisi',
        'legal_name': 'Free University of Tbilisi',
        'logo': open('tbilisi.png', 'rb').read(),
        'academic_authority': 'Test institute',
        'signer': {
            'names': [fake_en.first_name()],
            'surnames': [fake_en.last_name()],
            'role': 'Rector',
            'did': None,
            'title': fake_en.prefix()
            }
        }


main_verifier = {
        'verifier_id': '77398291-5d7a-44f5-ab15-2866a88125e8',
        'name': 'HR.GE',
        'logo': open('hrge.png', 'rb').read()
        }

def random_issuer(fake):
    city = fake.city()
    name = "University of {}".format(city)
    issuer_id = fake.uuid4(cast_to=str)

    return {
        'issuer_id': issuer_id,
        'did': 'did:atala:{}'.format(sha256(issuer_id.encode('utf8')).hexdigest()),
        'groups': [{'group_id': fake.uuid4(cast_to=str), 'name': fake.job().split(',')[0]}],
        'name': name,
        'legal_name': name,
        'logo': random_icon(city[0] + 'U'),
        'academic_authority': 'Test institute',
        'signer': {
            'names': [fake.first_name()],
            'surnames': [fake.last_name()],
            'role': 'Rector',
            'did': None,
            'title': fake.prefix()
            }
        }


def random_verifier(fake):
    name = fake.company()
    return {
        'verifier_id': fake.uuid4(cast_to=str),
        'name': name,
        'logo': random_icon("".join([x[0] for x in name.split()[:2]]))
        }

if __name__ == '__main__':
    
    if len(argv) > 1 and argv[1] == 'test':
        is_demo = False
        print("Generating test data for testing")
    else:
        is_demo = True
        print("Generating test data for demo - less, simpler data will be generated")
        print("In order to generate more data use:\npython3 fake_data.py test")

    out = open('fake_data.sql', 'w')
    print("-- Autogenerated with fake_data.py - do not modify manually", file = out)
    print(file = out)

    random_issuers = [random_issuer(fake_en) for _ in range(8)] + [random_issuer(fake_ge) for _ in range(4)]
    random_verifiers = [random_verifier(fake_en) for _ in range(6)] + [random_verifier(fake_ge) for _ in range (3)]

    for issuer in ([main_issuer] + random_issuers):
        print_issuer(issuer, out)

    for verifier in ([main_verifier] + random_verifiers):
        print_verifier(verifier, out)
    if is_demo:
        random_holders = [random_holder(fake_ge) for _ in range(3)]
    else:
        random_holders = [random_holder(fake_en) for _ in range(20)] + [random_holder(fake_ge) for _ in range(10)]
    test_holders = [
            test_holder("Mark Griffin", "mark.griffin@iohk.io", "en_IE"),
            #test_holder("Jacek Kurkowski", "jacek.kurkowski@iohk.io", "pl_PL"),
            test_holder("Christos Loverdos", "christos.loverdos@iohk.io", "el_GR"),
            #test_holder("Alexis Hernandez", "alexis.hernandez@iohk.io", "es_MX"),
            #test_holder("Marcin Kurczych", "marcin.kurczych@iohk.io", "pl_PL"),
            #test_holder("Shailesh Patil", "shailesh.patil@iohk.io", "en_UK"),
            #test_holder("Ezequiel Postan", "ezequiel.postan@iohk.io", "es"),
            #test_holder("Jeremy Towson", "jeremy.townson@iohk.io", "en_UK"),
            test_holder("Noel Rimbert", "noel.rimbert@iohk.io", "fr_FR"),
            ]
    holders = random_holders + test_holders

    main_holder_tokens = []
    main_holder_connector_ids = []

    for i, holder in enumerate(holders):
        issuers = [main_issuer] + (random_issuers if i == 0 else [])
        verifiers = [main_verifier] + (random_verifiers if i == 0 else [])

        credentials = []
        tokens = []
        connector_ids = []

        for issuer in issuers:
            student = random_student(holder['fake'], holder, issuer)
            print_student(student, out)

            if randint(1, 100) <= 80:
                credential = random_credential(holder['fake'], issuer, student)
                credential_sent = student['connection_status'] == CONNECTED
                print_credential(credential, credential_sent, out)
                if credential_sent:
                    credentials.append(credential)
            if student['connection_status'] == INVITED:
                tokens.append(student['connection_token'])

            if student['connection_status'] == CONNECTED:
                connector_ids.append(student['connector_id'])

        for verifier in verifiers:
            individual = random_individual(holder['fake'], holder, verifier)
            print_individual(individual, out)

            for credential in credentials:
                if individual['connection_status'] == CONNECTED and randint(1, 100) <= 50:
                    print_verifier_sent_credential(credential, individual, out)

            if individual['connection_status'] == INVITED:
                tokens.append(individual['connection_token'])

            if individual['connection_status'] == CONNECTED:
                connector_ids.append(individual['connector_id'])
        if i == 0:
            main_holder_tokens = tokens
            main_holder_connector_ids = connector_ids

    info_out = open('fake_data.txt', 'w')
    print("Issuer id to use to test issuer portal: {}".format(main_issuer['issuer_id']), file=info_out)
    print("Verifier id to use to test issuer portal: {}".format(main_verifier['verifier_id']), file=info_out)
    print("Holder data to use for tests:", file=info_out)
    print("User ids for connector:", file=info_out)
    for connector_id in main_holder_connector_ids:
        print("* {}".format(connector_id), file=info_out)
    print("Connections tokens that haven't been scanned:", file=info_out)
    for token in main_holder_tokens:
        print("* {}".format(token), file=info_out)
    print("Done")
