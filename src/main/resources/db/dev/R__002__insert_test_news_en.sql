WITH seed(title_ua, title_en, content_en) AS (
    VALUES
        ('Що таке вісцеральна терапія?', 'What Is Visceral Therapy?', '**Visceral therapy** is a gentle manual technique aimed at normalizing the position, tone, and mobility of internal organs by working with the ligamentous system and fascia.

Key characteristics of the method:
- Light pressure, stretching, and oscillating movements are used.
- The work is performed through the anterior abdominal wall.
- The main goal is to restore normal function of the internal organs and harmonize the body as a whole.

The method is based on the idea that organs may shift or lose mobility due to stress, injuries, or chronic conditions. This, in turn, may affect the musculoskeletal system, the nervous system, and the endocrine system.'),

        ('Користь вісцерального масажу', 'Benefits of Visceral Massage', 'Regular use of visceral therapy may have a wide range of positive effects:

- **Improved digestion**: stimulation of gastrointestinal function.
- **Reduced back and neck pain**: by relieving muscle spasms caused by visceral dysfunction.
- **Better sleep and emotional well-being**.
- **Support for menstrual cycle regulation** and reproductive health.
- **Detoxification support**: activation of lymphatic and blood flow, improved metabolism.
- **Support for the immune system**.

> Visceral therapy is often used as a complementary method together with conventional medicine.'),

        ('Показання до вісцеральної терапії', 'Indications for Visceral Therapy', 'The method may be recommended for the following conditions:

- Chronic gastritis, colitis, and biliary dyskinesia.
- Liver and pancreatic disorders.
- Irritable bowel syndrome.
- Urinary system disorders.
- Adhesions in the abdominal cavity.
- Pain in the lower back, thoracic spine, and cervical spine.
- Headaches and vegetative-vascular dystonia.
- Menstrual cycle disorders and PMS.
- Stress, anxiety, and chronic fatigue.

> It is important to undergo a preliminary assessment in order to avoid possible risks.'),

        ('Протипоказання до вісцерального масажу', 'Contraindications for Visceral Massage', 'The procedure is not recommended in the following cases:

- Acute infectious diseases with fever.
- Oncological conditions.
- Internal bleeding.
- Thrombosis or thrombophlebitis.
- Mental health disorders in an active phase.
- Pregnancy, especially the first trimester or when contraindicated by a doctor.
- Postoperative period until complete healing.
- During menstruation.

Before starting a session, it is extremely important to receive permission from a doctor or have a consultation with a qualified specialist.'),

        ('Як проходить сеанс вісцеральної терапії?', 'How Does a Visceral Therapy Session Work?', 'A session is carried out in a calm atmosphere, most often while lying on the back.

### Main stages:
1. **Examination and abdominal palpation** — the specialist assesses tissue tone, organ displacement, and the presence of painful areas.
2. **Work with the organs** — gentle pressure, stretching, and vibration techniques are used.
3. **Completion of the session** — recommendations may be given regarding daily routine, breathing practices, or physical exercises.

A session usually lasts from 30 to 60 minutes. A course may include 5 to 10 procedures with a frequency of 1–2 times per week.'),

        ('Поєднання з іншими методами лікування', 'Combination with Other Treatment Methods', 'Visceral massage combines well with other forms of therapy:

- **Osteopathy** — working with the body at a deeper level.
- **Kinesiotherapy** — strengthening muscles and normalizing movement patterns.
- **Breathing practices** — improving diaphragm function.
- **Psychotherapy** — working with psychosomatic blocks.
- **Herbal therapy** — supporting the organs with herbs.

> A comprehensive approach helps achieve more stable and lasting results.'),

        ('Наукове підґрунтя та думки лікарів', 'Scientific Background and Doctors’ Opinions', 'Visceral therapy is increasingly being discussed and used in modern therapeutic practice:

- Some studies indicate a reduction in back pain after visceral manipulation.
- The relationship between visceral restrictions and functional disorders is being studied.
- Some doctors use visceral osteopathy techniques in sports rehabilitation.

> Although the method is considered alternative in some countries, it continues to develop in Ukraine due to its practical results.'),

        ('Після сеансу: що відчуває пацієнт?', 'After the Session: What Does the Patient Feel?', 'After a visceral massage session, the following effects may be observed:

- A feeling of lightness and warmth in the abdomen.
- Increased urination or intestinal peristalsis.
- Temporary aggravation of symptoms — a normal response to therapy.
- Improved sleep and mood.

**Recommendations after the procedure:**
- Drink plenty of water.
- Avoid heavy food for several hours.
- Avoid getting chilled.
- Listen to your body and rest if needed.

> If symptoms cause concern, contact a specialist.'),

        ('Часті питання про вісцеральний масаж', 'Frequently Asked Questions About Visceral Massage', '### Does it hurt?
Usually, no. The sensations may be unusual or slightly painful in tense areas, but the therapy should not cause severe pain.

### How long does a course last?
Depending on the individual condition, a course may include 5 to 10 procedures, sometimes more.

### Can it be done for children?
Yes, but only after consulting a doctor and only with a specialist who has experience working with children.

### When does the result appear?
After 1–3 procedures, pain may decrease, sleep may improve, and digestion may normalize.

### How much does a session cost?
The price depends on the city, the specialist’s qualification, and the duration of the session.')
)
UPDATE news existing
SET
    title_en = seed.title_en,
    content_en = seed.content_en
FROM seed
WHERE existing.title_ua = seed.title_ua;